package jadx.mcp.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import jadx.mcp.protocol.JsonRpc;
import jadx.mcp.tools.ToolRegistry;

/**
 * MCP server with Streamable HTTP transport (single endpoint '/mcp') based on JDK HTTP server.
 * <ul>
 * <li>POST: JSON-RPC messages, responses always sent as 'application/json'</li>
 * <li>GET: 405, server doesn't open standalone SSE stream (allowed by spec)</li>
 * <li>DELETE: terminate session</li>
 * </ul>
 */
public final class McpServer {
	private static final Logger LOG = LoggerFactory.getLogger(McpServer.class);

	private static final String HEADER_SESSION_ID = "Mcp-Session-Id";
	private static final String HEADER_PROTOCOL_VERSION = "MCP-Protocol-Version";
	private static final int HTTP_THREADS = 16;

	private final McpServerConfig config;
	private final ToolRegistry registry;

	private @Nullable HttpServer httpServer;
	private @Nullable ExecutorService httpExecutor;
	private @Nullable ToolExecutor toolExecutor;
	private @Nullable McpSessionManager sessions;
	private @Nullable McpProtocolHandler handler;
	private boolean loopbackOnly;

	public McpServer(McpServerConfig config, ToolRegistry registry) {
		this.config = config;
		this.registry = registry;
	}

	public synchronized void start() throws IOException {
		if (httpServer != null) {
			return;
		}
		InetAddress bindAddr = InetAddress.getByName(config.getHost());
		loopbackOnly = bindAddr.isLoopbackAddress();
		if (!loopbackOnly && config.getToken() == null) {
			throw new IOException("Authentication token is required when listening on non-loopback address: " + config.getHost());
		}
		HttpServer server = HttpServer.create(new InetSocketAddress(bindAddr, config.getPort()), 0);
		McpSessionManager sessionManager = new McpSessionManager(config.getMaxSessions(), config.getSessionIdleTimeoutMs());
		ToolExecutor executor = new ToolExecutor(config.getMaxConcurrentCalls());
		McpProtocolHandler protocolHandler = new McpProtocolHandler(config, registry, sessionManager, executor);

		AtomicInteger counter = new AtomicInteger();
		ThreadPoolExecutor httpPool = new ThreadPoolExecutor(HTTP_THREADS, HTTP_THREADS, 30, TimeUnit.SECONDS,
				new LinkedBlockingQueue<>(), r -> {
					Thread thread = new Thread(r, "jadx-mcp-http-" + counter.incrementAndGet());
					thread.setDaemon(true);
					return thread;
				});
		httpPool.allowCoreThreadTimeOut(true);
		server.setExecutor(httpPool);
		server.createContext("/", exchange -> handleExchange(exchange, protocolHandler));
		server.start();

		this.httpServer = server;
		this.httpExecutor = httpPool;
		this.toolExecutor = executor;
		this.sessions = sessionManager;
		this.handler = protocolHandler;
		LOG.info("MCP server started at {}", getEndpointUrl());
	}

	public synchronized void stop() {
		HttpServer server = httpServer;
		if (server == null) {
			return;
		}
		if (handler != null) {
			handler.stop();
		}
		server.stop(0);
		if (httpExecutor != null) {
			httpExecutor.shutdown();
		}
		if (toolExecutor != null) {
			toolExecutor.shutdown();
		}
		httpServer = null;
		httpExecutor = null;
		toolExecutor = null;
		sessions = null;
		handler = null;
		LOG.info("MCP server stopped");
	}

	public synchronized boolean isRunning() {
		return httpServer != null;
	}

	public McpServerConfig getConfig() {
		return config;
	}

	public synchronized @Nullable InetSocketAddress getBoundAddress() {
		return httpServer == null ? null : httpServer.getAddress();
	}

	public synchronized String getEndpointUrl() {
		InetSocketAddress addr = getBoundAddress();
		int port = addr != null ? addr.getPort() : config.getPort();
		return buildEndpointUrl(config.getHost(), port);
	}

	public static String buildEndpointUrl(String host, int port) {
		String urlHost = host;
		if ("0.0.0.0".equals(host) || "::".equals(host)) {
			urlHost = "127.0.0.1";
		} else if (host.contains(":") && !host.startsWith("[")) {
			urlHost = '[' + host + ']';
		}
		return "http://" + urlHost + ':' + port + McpServerConfig.ENDPOINT_PATH;
	}

	public synchronized int getSessionsCount() {
		return sessions == null ? 0 : sessions.size();
	}

	public synchronized @Nullable McpProtocolHandler getHandler() {
		return handler;
	}

	private void handleExchange(HttpExchange exchange, McpProtocolHandler protocolHandler) {
		try {
			String path = exchange.getRequestURI().getPath();
			if (!McpServerConfig.ENDPOINT_PATH.equals(path) && !(McpServerConfig.ENDPOINT_PATH + '/').equals(path)) {
				sendError(exchange, 404, "not_found", "Unknown path. MCP endpoint: " + McpServerConfig.ENDPOINT_PATH);
				return;
			}
			Headers headers = exchange.getRequestHeaders();
			if (!checkHost(headers.getFirst("Host"))) {
				sendError(exchange, 403, "forbidden", "Invalid Host header");
				return;
			}
			if (!checkOrigin(headers.getFirst("Origin"))) {
				sendError(exchange, 403, "forbidden", "Origin not allowed");
				return;
			}
			if (!checkAuth(headers.getFirst("Authorization"))) {
				exchange.getResponseHeaders().set("WWW-Authenticate", "Bearer realm=\"jadx-mcp\"");
				sendError(exchange, 401, "unauthorized",
						"Missing or invalid token. Send 'Authorization: Bearer <token>' (see jadx Preferences > MCP Server).");
				return;
			}
			String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
			switch (method) {
				case "POST":
					handlePost(exchange, protocolHandler);
					break;
				case "DELETE":
					writeResponse(exchange, protocolHandler.handleDelete(headers.getFirst(HEADER_SESSION_ID)));
					break;
				case "GET":
					// standalone SSE stream not supported (server doesn't send notifications)
					exchange.getResponseHeaders().set("Allow", "POST, DELETE");
					sendError(exchange, 405, "method_not_allowed", "SSE stream not supported, use POST");
					break;
				default:
					exchange.getResponseHeaders().set("Allow", "POST, DELETE");
					sendError(exchange, 405, "method_not_allowed", "Method not allowed: " + method);
					break;
			}
		} catch (Exception e) {
			LOG.warn("MCP HTTP request processing error", e);
			try {
				sendError(exchange, 500, "internal_error", String.valueOf(e.getMessage()));
			} catch (Exception ignore) {
				// response already sent or connection closed
			}
		} finally {
			exchange.close();
		}
	}

	private void handlePost(HttpExchange exchange, McpProtocolHandler protocolHandler) throws IOException {
		Headers headers = exchange.getRequestHeaders();
		String contentType = headers.getFirst("Content-Type");
		if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("application/json")) {
			sendError(exchange, 415, "unsupported_media_type", "Content-Type must be application/json");
			return;
		}
		if (!isJsonAccepted(headers.get("Accept"))) {
			sendError(exchange, 406, "not_acceptable", "Accept header must allow application/json");
			return;
		}
		byte[] body = readBody(exchange.getRequestBody(), config.getMaxRequestBytes());
		if (body == null) {
			sendError(exchange, 413, "payload_too_large", "Request body exceeds " + config.getMaxRequestBytes() + " bytes");
			return;
		}
		JsonElement message;
		try {
			message = JsonParser.parseString(new String(body, StandardCharsets.UTF_8));
		} catch (JsonParseException e) {
			writeJson(exchange, 400, JsonRpc.error(null, JsonRpc.PARSE_ERROR, "Parse error: " + e.getMessage()), null);
			return;
		}
		if (message == null || message.isJsonNull()) {
			writeJson(exchange, 400, JsonRpc.error(null, JsonRpc.INVALID_REQUEST, "Empty request"), null);
			return;
		}
		McpProtocolHandler.Response response = protocolHandler.handlePost(
				headers.getFirst(HEADER_SESSION_ID), headers.getFirst(HEADER_PROTOCOL_VERSION), message);
		writeResponse(exchange, response);
	}

	private static boolean isJsonAccepted(@Nullable List<String> acceptHeaders) {
		if (acceptHeaders == null || acceptHeaders.isEmpty()) {
			return true;
		}
		for (String accept : acceptHeaders) {
			String lower = accept.toLowerCase(Locale.ROOT);
			if (lower.contains("application/json") || lower.contains("*/*") || lower.contains("application/*")) {
				return true;
			}
		}
		return false;
	}

	private static @Nullable byte[] readBody(InputStream in, int limit) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[8192];
		int total = 0;
		int read;
		while ((read = in.read(buf)) != -1) {
			total += read;
			if (total > limit) {
				return null;
			}
			out.write(buf, 0, read);
		}
		return out.toByteArray();
	}

	private void writeResponse(HttpExchange exchange, McpProtocolHandler.Response response) throws IOException {
		JsonElement body = response.getBody();
		if (body == null) {
			exchange.getResponseHeaders().set("Cache-Control", "no-store");
			exchange.sendResponseHeaders(response.getStatus(), -1);
			return;
		}
		writeJson(exchange, response.getStatus(), body, response.getSessionId());
	}

	private static void writeJson(HttpExchange exchange, int status, JsonElement body, @Nullable String sessionId) throws IOException {
		byte[] bytes = JsonRpc.GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
		Headers respHeaders = exchange.getResponseHeaders();
		respHeaders.set("Content-Type", "application/json; charset=utf-8");
		respHeaders.set("Cache-Control", "no-store");
		if (sessionId != null) {
			respHeaders.set(HEADER_SESSION_ID, sessionId);
		}
		exchange.sendResponseHeaders(status, bytes.length);
		try (OutputStream os = exchange.getResponseBody()) {
			os.write(bytes);
		}
	}

	private static void sendError(HttpExchange exchange, int status, String error, String message) throws IOException {
		JsonObject obj = new JsonObject();
		obj.addProperty("error", error);
		obj.addProperty("message", message);
		writeJson(exchange, status, obj, null);
	}

	private boolean checkAuth(@Nullable String authHeader) {
		String token = config.getToken();
		if (token == null) {
			return true;
		}
		if (authHeader == null) {
			return false;
		}
		String prefix = "bearer ";
		if (authHeader.length() <= prefix.length()
				|| !authHeader.substring(0, prefix.length()).toLowerCase(Locale.ROOT).equals(prefix)) {
			return false;
		}
		byte[] provided = authHeader.substring(prefix.length()).trim().getBytes(StandardCharsets.UTF_8);
		return MessageDigest.isEqual(provided, token.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Protection against DNS rebinding: for loopback listener allow only local host names
	 */
	private boolean checkHost(@Nullable String hostHeader) {
		if (!loopbackOnly || hostHeader == null) {
			return true;
		}
		return isLocalHostName(stripPort(hostHeader.trim()));
	}

	/**
	 * Browsers always send Origin: allow only local pages. Non-browser clients don't send Origin.
	 */
	private static boolean checkOrigin(@Nullable String origin) {
		if (origin == null || origin.isEmpty()) {
			return true;
		}
		try {
			URI uri = new URI(origin.trim());
			String host = uri.getHost();
			return host != null && isLocalHostName(host);
		} catch (Exception e) {
			return false;
		}
	}

	static String stripPort(String hostPort) {
		if (hostPort.startsWith("[")) {
			int end = hostPort.indexOf(']');
			return end == -1 ? hostPort : hostPort.substring(0, end + 1);
		}
		int colon = hostPort.indexOf(':');
		if (colon != -1 && colon == hostPort.lastIndexOf(':')) {
			return hostPort.substring(0, colon);
		}
		return hostPort;
	}

	static boolean isLocalHostName(String host) {
		String h = host.toLowerCase(Locale.ROOT);
		if (h.startsWith("[") && h.endsWith("]")) {
			h = h.substring(1, h.length() - 1);
		}
		return h.equals("localhost")
				|| h.endsWith(".localhost")
				|| h.equals("::1")
				|| h.equals("0:0:0:0:0:0:0:1")
				|| h.matches("127\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
	}
}
