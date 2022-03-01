package jadx.gui.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxDecompiler;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.settings.JadxSettings;
import jadx.gui.settings.JadxSettingsAdapter;
import jadx.gui.utils.LafManager;
import jadx.gui.utils.Link;

public class ExceptionDialog extends JDialog {

	private static final Logger LOG = LoggerFactory.getLogger(ExceptionDialog.class);

	private static final String FMT_DETAIL_LENGTH = "-13";

	public static void registerUncaughtExceptionHandler() {
		Thread.setDefaultUncaughtExceptionHandler((thread, ex) -> showExceptionDialog(thread, ex));
	}

	public static void showExceptionDialog(Thread thread, Throwable ex) {
		LOG.error("Exception was thrown", ex);
		new ExceptionDialog(thread, ex);
	}

	public ExceptionDialog(Thread thread, Throwable ex) {
		super((Window) null, "Jadx Error");
		this.getContentPane().setLayout(new BorderLayout());
		JPanel titlePanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.CENTER;
		c.gridx = 0;
		c.weightx = 1.0;
		c.insets = new Insets(2, 5, 5, 5);
		JLabel titleLabel = new JLabel("<html><h1>An error occurred</h1><p>Jadx encountered an unexpected error.</p></html>");

		Map<String, String> details = new LinkedHashMap<>();
		details.put("Jadx version", JadxDecompiler.getVersion());
		details.put("Java version", System.getProperty("java.version", "?"));
		details.put("Java VM", String.format("%s %s", System.getProperty("java.vm.vendor", "?"),
				System.getProperty("java.vm.name", "?")));
		details.put("Platform", String.format("%s (%s %s)", System.getProperty("os.name", "?"),
				System.getProperty("os.version", "?"), System.getProperty("os.arch", "?")));
		Runtime runtime = Runtime.getRuntime();
		details.put("Max heap size", String.format("%d MB", runtime.maxMemory() / (1024 * 1024)));

		try {
			// TODO: Use ProcessHandle.current().info().commandLine() once min Java is 9+
			List<String> args = ManagementFactory.getRuntimeMXBean().getInputArguments();
			details.put("Program args", args.stream().collect(Collectors.joining(" ")));
		} catch (Throwable t) {
			LOG.error("failed to get program arguments", t);
		}

		StringWriter stackTraceWriter = new StringWriter(1024);
		ex.printStackTrace(new PrintWriter(stackTraceWriter));
		final String stackTrace = stackTraceWriter.toString();

		String issueTitle;
		try {
			issueTitle = URLEncoder.encode(ex.toString(), StandardCharsets.UTF_8.toString());
		} catch (Exception e) {
			LOG.error("URL encoding of title failed", e);
			issueTitle = ex.getClass().getSimpleName();
		}

		String message = "Please describe what you did before the error occurred.\n";
		message += "**IMPORTANT!** If the error occurs with a specific APK file please attach or provide link to apk file!\n";

		StringBuilder detailsIssueBuilder = new StringBuilder();
		details.forEach((key, value) -> detailsIssueBuilder.append(String.format("* %s: %s\n", key, value)));

		String body = String.format("%s %s\n```\n%s\n```", message, detailsIssueBuilder, stackTrace);

		String issueBody;
		try {
			issueBody = URLEncoder.encode(body, StandardCharsets.UTF_8.toString());
		} catch (Exception e) {
			LOG.error("URL encoding of body failed", e);
			issueBody = "Please copy the displayed text in the Jadx error dialog and paste it here";
		}

		String url = String.format("https://github.com/skylot/jadx/issues/new?labels=bug&title=%s&body=%s", issueTitle, issueBody);
		Link issueLink = new Link("<html><u><b>Create a new issue at GitHub</b></u></html>", url);
		c.gridy = 0;
		titlePanel.add(titleLabel, c);
		c.gridy = 1;
		titlePanel.add(issueLink, c);
		JTextArea messageArea = new JTextArea();
		messageArea.setEditable(false);
		messageArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		messageArea.setForeground(Color.BLACK);
		messageArea.setBackground(Color.WHITE);

		StringBuilder detailsTextBuilder = new StringBuilder();
		details.forEach((key, value) -> detailsTextBuilder.append(String.format("%" + FMT_DETAIL_LENGTH + "s: %s\n", key, value)));

		messageArea.setText(detailsTextBuilder.toString() + "\n" + stackTrace);

		JPanel buttonPanel = new JPanel();
		JButton exitButton = new JButton("Terminate Jadx");
		exitButton.addActionListener((event) -> System.exit(1));
		buttonPanel.add(exitButton);
		JButton closeButton = new JButton("Go back to Jadx");
		closeButton.addActionListener((event) -> setVisible(false));
		buttonPanel.add(closeButton);
		JScrollPane messageAreaScroller = new JScrollPane(messageArea);
		messageAreaScroller.setMinimumSize(new Dimension(600, 400));
		messageAreaScroller.setPreferredSize(new Dimension(600, 400));

		this.add(titlePanel, BorderLayout.NORTH);
		this.add(messageAreaScroller, BorderLayout.CENTER);
		this.add(buttonPanel, BorderLayout.SOUTH);
		this.pack();

		javax.swing.SwingUtilities.invokeLater(() -> messageAreaScroller.getVerticalScrollBar().setValue(0));

		final Toolkit toolkit = Toolkit.getDefaultToolkit();
		final Dimension screenSize = toolkit.getScreenSize();
		final int x = (screenSize.width - getWidth()) / 2;
		final int y = (screenSize.height - getHeight()) / 2;
		setLocation(x, y);

		getRootPane().registerKeyboardAction((event) -> setVisible(false),
				KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
				JComponent.WHEN_IN_FOCUSED_WINDOW);

		this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		this.setVisible(true);
	}

	public static void throwTestException() {
		try {
			throw new RuntimeException("Inner exception message");
		} catch (Exception e) {
			throw new JadxRuntimeException("Outer exception message", e);
		}
	}

	public static void showTestExceptionDialog() {
		try {
			throwTestException();
		} catch (Exception e) {
			showExceptionDialog(Thread.currentThread(), e);
		}
	}

	public static void main(String[] args) {
		JadxSettings settings = JadxSettingsAdapter.load();
		LafManager.init(settings);
		showTestExceptionDialog();
	}
}
