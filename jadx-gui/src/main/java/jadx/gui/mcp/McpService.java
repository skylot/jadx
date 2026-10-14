package jadx.gui.mcp;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxDecompiler;
import jadx.gui.settings.JadxSettings;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.UiUtils;
import jadx.mcp.jadx.JadxMcpTools;
import jadx.mcp.project.ProjectGate;
import jadx.mcp.server.McpProtocolHandler;
import jadx.mcp.server.McpServer;
import jadx.mcp.server.McpServerConfig;
import jadx.mcp.tools.ToolRegistry;

/**
 * Owner of built-in MCP server: start/stop by settings, project lifecycle notifications.
 * Server lives for the whole application, project changes only switch backend state.
 */
public class McpService {
	private static final Logger LOG = LoggerFactory.getLogger(McpService.class);

	private static final long PROJECT_CLOSE_WAIT_MS = 10_000;

	private final MainWindow mainWindow;
	private final JadxSettings settings;
	private final GuiMcpBackend backend;
	private final ToolRegistry registry;
	private final ExecutorService controlExecutor;

	private volatile @Nullable McpServer server;
	private volatile @Nullable McpServerConfig activeConfig;
	private volatile @Nullable String lastError;

	public McpService(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
		this.settings = mainWindow.getSettings();
		this.backend = new GuiMcpBackend(mainWindow);
		this.registry = JadxMcpTools.createRegistry(backend, settings::isMcpWriteToolsEnabled);
		this.controlExecutor = Executors.newSingleThreadExecutor(r -> {
			Thread thread = new Thread(r, "jadx-mcp-control");
			thread.setDaemon(true);
			return thread;
		});
	}

	/**
	 * Start server on application start if enabled
	 */
	public void init() {
		if (settings.isMcpServerEnabled()) {
			applySettings();
		}
	}

	/**
	 * Start, stop or restart server to match current settings (async)
	 */
	public Future<?> applySettings() {
		return controlExecutor.submit(this::syncWithSettings);
	}

	/**
	 * Force restart with current settings (async)
	 */
	public Future<?> restart() {
		return controlExecutor.submit(() -> {
			stopServer();
			syncWithSettings();
		});
	}

	private synchronized void syncWithSettings() {
		if (!settings.isMcpServerEnabled()) {
			stopServer();
			lastError = null;
			return;
		}
		McpServerConfig config;
		try {
			config = buildConfig();
		} catch (Exception e) {
			stopServer();
			lastError = e.getMessage();
			return;
		}
		McpServer current = server;
		if (current != null && current.isRunning() && sameConfig(activeConfig, config)) {
			return;
		}
		stopServer();
		McpServer newServer = new McpServer(config, registry);
		try {
			newServer.start();
			server = newServer;
			activeConfig = config;
			lastError = null;
		} catch (Exception e) {
			LOG.error("Failed to start MCP server on {}:{}", config.getHost(), config.getPort(), e);
			lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
		}
	}

	private McpServerConfig buildConfig() {
		String token = null;
		if (settings.isMcpRequireToken()) {
			token = ensureToken();
		}
		return McpServerConfig.builder()
				.host(settings.getMcpServerHost().trim())
				.port(settings.getMcpServerPort())
				.token(token)
				.toolTimeoutMs(Math.max(5, settings.getMcpToolTimeout()) * 1000L)
				.serverVersion(JadxDecompiler.getVersion())
				.instructions(JadxMcpTools.INSTRUCTIONS)
				.build();
	}

	/**
	 * Generate access token on first use
	 */
	public String ensureToken() {
		String token = settings.getMcpToken();
		if (token == null || token.trim().isEmpty()) {
			token = generateToken();
			settings.setMcpToken(token);
			String newToken = token;
			UiUtils.uiRun(() -> {
				if (!mainWindow.isSettingsOpen()) {
					settings.sync();
				}
				LOG.debug("MCP access token generated, length: {}", newToken.length());
			});
		}
		return token.trim();
	}

	public static String generateToken() {
		byte[] bytes = new byte[24];
		new SecureRandom().nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private static boolean sameConfig(@Nullable McpServerConfig a, McpServerConfig b) {
		return a != null
				&& a.getHost().equals(b.getHost())
				&& a.getPort() == b.getPort()
				&& Objects.equals(a.getToken(), b.getToken())
				&& a.getToolTimeoutMs() == b.getToolTimeoutMs();
	}

	private void stopServer() {
		McpServer current = server;
		if (current != null) {
			current.stop();
			server = null;
			activeConfig = null;
		}
	}

	/**
	 * Current state for settings page
	 */
	public McpStatus getStatus() {
		McpServer current = server;
		if (current != null && current.isRunning()) {
			McpProtocolHandler handler = current.getHandler();
			return new McpStatus(true, current.getEndpointUrl(), current.getSessionsCount(),
					handler == null ? 0 : handler.getToolCallsCount(),
					handler == null ? null : handler.getLastClient(),
					null, backend.getGate().getState());
		}
		return new McpStatus(false, null, 0, 0, null, lastError, backend.getGate().getState());
	}

	public ToolRegistry getRegistry() {
		return registry;
	}

	// project lifecycle (called by MainWindow)

	public void projectLoading() {
		backend.getGate().setLoading();
	}

	public void projectLoaded() {
		mainWindow.getWrapper().getCurrentDecompiler().ifPresentOrElse(
				decompiler -> backend.getGate().setReady(decompiler),
				() -> backend.getGate().setEmpty());
	}

	public void projectLoadFailed() {
		backend.getGate().setEmpty();
	}

	/**
	 * Reject new requests and wait for running ones. Must be called before decompiler close, not on UI
	 * thread.
	 */
	public void projectClosing() {
		ProjectGate gate = backend.getGate();
		if (gate.getState() == ProjectGate.State.EMPTY) {
			return;
		}
		if (SwingUtilities.isEventDispatchThread()) {
			LOG.warn("MCP project close called on UI thread, skip waiting for running requests");
			gate.close(0);
			return;
		}
		gate.close(PROJECT_CLOSE_WAIT_MS);
	}

	/**
	 * Application exit
	 */
	public void shutdown() {
		backend.getGate().close(SwingUtilities.isEventDispatchThread() ? 0 : 2000);
		Future<?> stop = controlExecutor.submit(this::stopServer);
		try {
			stop.get(5, TimeUnit.SECONDS);
		} catch (Exception e) {
			LOG.warn("MCP server stop failed", e);
		}
		controlExecutor.shutdown();
	}

	/**
	 * Immutable status snapshot
	 */
	public static final class McpStatus {
		private final boolean running;
		private final @Nullable String url;
		private final int sessions;
		private final long toolCalls;
		private final @Nullable String lastClient;
		private final @Nullable String error;
		private final ProjectGate.State projectState;

		McpStatus(boolean running, @Nullable String url, int sessions, long toolCalls,
				@Nullable String lastClient, @Nullable String error, ProjectGate.State projectState) {
			this.running = running;
			this.url = url;
			this.sessions = sessions;
			this.toolCalls = toolCalls;
			this.lastClient = lastClient;
			this.error = error;
			this.projectState = projectState;
		}

		public boolean isRunning() {
			return running;
		}

		public @Nullable String getUrl() {
			return url;
		}

		public int getSessions() {
			return sessions;
		}

		public long getToolCalls() {
			return toolCalls;
		}

		public @Nullable String getLastClient() {
			return lastClient;
		}

		public @Nullable String getError() {
			return error;
		}

		public ProjectGate.State getProjectState() {
			return projectState;
		}
	}
}
