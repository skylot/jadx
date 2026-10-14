package jadx.gui.settings.ui.mcp;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ItemEvent;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.gui.ISettingsGroup;
import jadx.gui.mcp.McpService;
import jadx.gui.settings.JadxSettings;
import jadx.gui.settings.ui.JadxSettingsWindow;
import jadx.gui.settings.ui.SettingsGroup;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.ui.DocumentUpdateListener;
import jadx.mcp.server.McpServer;
import jadx.mcp.server.McpServerConfig;

/**
 * Settings and status of built-in MCP server
 */
public class McpSettingsGroup implements ISettingsGroup {
	private static final String[] HOSTS = { "127.0.0.1", "localhost", "0.0.0.0" };

	private final JadxSettings settings;
	private final McpService mcpService;
	private final String title = NLS.str("preferences.mcp");

	private @Nullable Timer statusTimer;
	private JLabel statusLabel;
	private JLabel warningLabel;
	private JTextField tokenField;
	private JTextArea configArea;

	public McpSettingsGroup(JadxSettingsWindow settingsWindow, JadxSettings settings) {
		this.settings = settings;
		this.mcpService = settingsWindow.getMainWindow().getMcpService();
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public JComponent buildComponent() {
		// options and status on top (full width), client config fills remaining space
		JPanel topPanel = new JPanel(new BorderLayout(0, 5));
		topPanel.add(buildOptions().buildComponent(), BorderLayout.PAGE_START);
		topPanel.add(buildStatusPanel(), BorderLayout.CENTER);

		JPanel mainPanel = new JPanel(new BorderLayout(0, 5));
		mainPanel.add(topPanel, BorderLayout.PAGE_START);
		mainPanel.add(buildConfigPanel(), BorderLayout.CENTER);
		updateDerived();
		startStatusTimer();
		return mainPanel;
	}

	private SettingsGroup buildOptions() {
		JCheckBox enable = new JCheckBox();
		enable.setSelected(settings.isMcpServerEnabled());
		enable.addItemListener(e -> settings.setMcpServerEnabled(e.getStateChange() == ItemEvent.SELECTED));

		JComboBox<String> hostCbx = new JComboBox<>(HOSTS);
		hostCbx.setEditable(true);
		hostCbx.setSelectedItem(settings.getMcpServerHost());
		hostCbx.addActionListener(e -> {
			Object selected = hostCbx.getSelectedItem();
			if (selected != null && !selected.toString().trim().isEmpty()) {
				settings.setMcpServerHost(selected.toString().trim());
				updateDerived();
			}
		});

		JSpinner portSpinner = new JSpinner(new SpinnerNumberModel(settings.getMcpServerPort(), 1, 65535, 1));
		portSpinner.setEditor(new JSpinner.NumberEditor(portSpinner, "#"));
		portSpinner.addChangeListener(e -> {
			settings.setMcpServerPort((Integer) portSpinner.getValue());
			updateDerived();
		});

		JCheckBox requireToken = new JCheckBox();
		requireToken.setSelected(settings.isMcpRequireToken());
		requireToken.addItemListener(e -> {
			settings.setMcpRequireToken(e.getStateChange() == ItemEvent.SELECTED);
			updateDerived();
		});

		tokenField = new JTextField(22);
		tokenField.setText(settings.getMcpToken());
		tokenField.setToolTipText(NLS.str("preferences.mcp.token_hint"));
		tokenField.getDocument().addDocumentListener(new DocumentUpdateListener(ev -> {
			settings.setMcpToken(tokenField.getText().trim());
			updateDerived();
		}));
		JButton regenerate = new JButton(NLS.str("preferences.mcp.token_regenerate"));
		regenerate.addActionListener(e -> tokenField.setText(McpService.generateToken()));
		JButton copyToken = new JButton(NLS.str("preferences.mcp.copy"));
		copyToken.addActionListener(e -> copy(settings.getMcpToken()));
		JPanel tokenPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		tokenPanel.add(tokenField);
		tokenPanel.add(regenerate);
		tokenPanel.add(copyToken);

		JCheckBox writeTools = new JCheckBox();
		writeTools.setSelected(settings.isMcpWriteToolsEnabled());
		writeTools.addItemListener(e -> settings.setMcpWriteToolsEnabled(e.getStateChange() == ItemEvent.SELECTED));

		JSpinner timeoutSpinner = new JSpinner(new SpinnerNumberModel(
				Math.max(5, Math.min(3600, settings.getMcpToolTimeout())), 5, 3600, 5));
		timeoutSpinner.addChangeListener(e -> settings.setMcpToolTimeout((Integer) timeoutSpinner.getValue()));

		SettingsGroup group = new SettingsGroup(title);
		group.addRow(NLS.str("preferences.mcp.enable"), enable);
		group.addRow(NLS.str("preferences.mcp.host"), hostCbx);
		group.addRow(NLS.str("preferences.mcp.port"), portSpinner);
		group.addRow(NLS.str("preferences.mcp.require_token"), requireToken);
		group.addRow(NLS.str("preferences.mcp.token"), tokenPanel);
		group.addRow(NLS.str("preferences.mcp.write_tools"), writeTools);
		group.addRow(NLS.str("preferences.mcp.timeout"), timeoutSpinner);
		group.end();
		return group;
	}

	private JPanel buildStatusPanel() {
		statusLabel = new JLabel();
		warningLabel = new JLabel();
		warningLabel.setForeground(new Color(0xD0, 0x50, 0x30));

		JButton applyBtn = new JButton(NLS.str("preferences.mcp.apply"));
		applyBtn.addActionListener(e -> {
			ensureTokenInUi();
			mcpService.restart();
		});
		JButton copyUrl = new JButton(NLS.str("preferences.mcp.copy_url"));
		copyUrl.addActionListener(e -> copy(endpointUrl()));

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		buttons.add(applyBtn);
		buttons.add(copyUrl);

		JPanel labels = new JPanel(new GridLayout(0, 1, 0, 2));
		labels.add(statusLabel);
		labels.add(warningLabel);

		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(BorderFactory.createTitledBorder(NLS.str("preferences.mcp.status")));
		panel.add(labels, BorderLayout.CENTER);
		panel.add(buttons, BorderLayout.PAGE_END);
		return panel;
	}

	private JPanel buildConfigPanel() {
		configArea = new JTextArea(12, 0);
		configArea.setEditable(false);
		configArea.setFont(settings.getCodeFont());

		JButton copyConfig = new JButton(NLS.str("preferences.mcp.copy_config"));
		copyConfig.addActionListener(e -> copy(buildJsonConfig()));
		JButton copyClaude = new JButton(NLS.str("preferences.mcp.copy_claude"));
		copyClaude.addActionListener(e -> copy(buildClaudeCommand()));
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
		buttons.add(copyConfig);
		buttons.add(copyClaude);

		JPanel panel = new JPanel(new BorderLayout(5, 5));
		panel.setBorder(BorderFactory.createTitledBorder(NLS.str("preferences.mcp.client_config")));
		JScrollPane configScroll = new JScrollPane(configArea);
		// long lines scroll inside text area instead of widening the settings page
		configScroll.setPreferredSize(new Dimension(200, 220));
		panel.add(configScroll, BorderLayout.CENTER);
		panel.add(buttons, BorderLayout.PAGE_END);
		return panel;
	}

	private void ensureTokenInUi() {
		if (settings.isMcpRequireToken() && settings.getMcpToken().trim().isEmpty()) {
			String token = McpService.generateToken();
			settings.setMcpToken(token);
			// page may be never opened (components not created)
			if (tokenField != null) {
				tokenField.setText(token);
			}
		}
	}

	private String endpointUrl() {
		return McpServer.buildEndpointUrl(settings.getMcpServerHost(), settings.getMcpServerPort());
	}

	private @Nullable String activeToken() {
		if (!settings.isMcpRequireToken()) {
			return null;
		}
		String token = settings.getMcpToken().trim();
		return token.isEmpty() ? "<token generated on start>" : token;
	}

	private String buildJsonConfig() {
		String token = activeToken();
		StringBuilder sb = new StringBuilder();
		sb.append("{\n");
		sb.append("  \"mcpServers\": {\n");
		sb.append("    \"jadx\": {\n");
		sb.append("      \"type\": \"http\",\n");
		sb.append("      \"url\": \"").append(endpointUrl()).append('"');
		if (token != null) {
			sb.append(",\n      \"headers\": {\n");
			sb.append("        \"Authorization\": \"Bearer ").append(token).append("\"\n");
			sb.append("      }\n");
		} else {
			sb.append('\n');
		}
		sb.append("    }\n");
		sb.append("  }\n");
		sb.append('}');
		return sb.toString();
	}

	private String buildClaudeCommand() {
		String token = activeToken();
		StringBuilder sb = new StringBuilder();
		sb.append("claude mcp add --transport http jadx ").append(endpointUrl());
		if (token != null) {
			sb.append(" --header \"Authorization: Bearer ").append(token).append('"');
		}
		return sb.toString();
	}

	private void updateDerived() {
		if (configArea == null || tokenField == null) {
			return;
		}
		boolean requireToken = settings.isMcpRequireToken();
		tokenField.setEnabled(requireToken);
		configArea.setText(buildJsonConfig() + "\n\n# Claude Code:\n" + buildClaudeCommand());
		configArea.setCaretPosition(0);
		String token = settings.getMcpToken().trim();
		// empty token will be generated on start
		boolean tokenInvalid = requireToken && !token.isEmpty() && !McpServerConfig.isValidToken(token);
		UiUtils.highlightAsErrorField(tokenField, tokenInvalid);
		String host = settings.getMcpServerHost().trim().toLowerCase(Locale.ROOT);
		boolean local = host.equals("127.0.0.1") || host.equals("localhost") || host.equals("::1") || host.equals("[::1]");
		if (tokenInvalid) {
			warningLabel.setText(NLS.str("preferences.mcp.token_invalid"));
		} else {
			warningLabel.setText(local ? " " : NLS.str("preferences.mcp.remote_warning"));
		}
	}

	private void startStatusTimer() {
		// page component rebuilt on each page selection
		stopStatusTimer();
		updateStatus();
		Timer timer = new Timer(1000, e -> {
			if (!statusLabel.isDisplayable()) {
				// page replaced (settings reset) or window disposed
				stopStatusTimer();
				return;
			}
			updateStatus();
		});
		statusTimer = timer;
		timer.start();
	}

	private void stopStatusTimer() {
		Timer timer = statusTimer;
		if (timer != null) {
			timer.stop();
			statusTimer = null;
		}
	}

	private void updateStatus() {
		McpService.McpStatus status = mcpService.getStatus();
		String text;
		if (status.isRunning()) {
			text = NLS.str("preferences.mcp.status_running", status.getUrl(), status.getSessions(), status.getToolCalls());
			if (status.getLastClient() != null) {
				text += " (" + status.getLastClient() + ')';
			}
		} else if (status.getError() != null) {
			text = NLS.str("preferences.mcp.status_error", status.getError());
		} else {
			text = NLS.str("preferences.mcp.status_stopped");
		}
		statusLabel.setText(text + "  |  " + NLS.str("preferences.mcp.project_state", status.getProjectState()));
	}

	private void copy(String text) {
		UiUtils.copyToClipboard(text);
		statusLabel.setText(NLS.str("preferences.mcp.copied"));
	}

	@Override
	public void close(boolean save) {
		stopStatusTimer();
		ensureTokenInUi();
		if (save) {
			// settings window saves all settings to disk after groups close
			mcpService.applySettings();
			return;
		}
		// Settings window 'cancel' (also window close button and Esc) restores previous values of all
		// settings.
		// MCP settings work as a control panel: keep and persist them on any close.
		McpValues values = McpValues.from(settings);
		SwingUtilities.invokeLater(() -> {
			values.applyTo(settings);
			settings.sync();
			mcpService.applySettings();
		});
	}

	/**
	 * Snapshot of MCP options
	 */
	private static final class McpValues {
		private final boolean enabled;
		private final String host;
		private final int port;
		private final boolean requireToken;
		private final String token;
		private final boolean writeTools;
		private final int timeout;

		private McpValues(JadxSettings s) {
			this.enabled = s.isMcpServerEnabled();
			this.host = s.getMcpServerHost();
			this.port = s.getMcpServerPort();
			this.requireToken = s.isMcpRequireToken();
			this.token = s.getMcpToken();
			this.writeTools = s.isMcpWriteToolsEnabled();
			this.timeout = s.getMcpToolTimeout();
		}

		static McpValues from(JadxSettings settings) {
			return new McpValues(settings);
		}

		void applyTo(JadxSettings s) {
			s.setMcpServerEnabled(enabled);
			s.setMcpServerHost(host);
			s.setMcpServerPort(port);
			s.setMcpRequireToken(requireToken);
			s.setMcpToken(token);
			s.setMcpWriteToolsEnabled(writeTools);
			s.setMcpToolTimeout(timeout);
		}
	}
}
