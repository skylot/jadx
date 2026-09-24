package jadx.gui.ui.ai;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import jadx.gui.ai.AiChatMessage;
import jadx.gui.ai.AiClient;
import jadx.gui.ai.AiSettings;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;

/**
 * Simple chat panel talking to a configured AI provider (see {@link AiSettings}).
 * The whole conversation is kept in memory only, nothing is persisted.
 */
public class AiAssistantPanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private final MainWindow mainWindow;
	private final List<AiChatMessage> history = new ArrayList<>();

	private JTextArea chatArea;
	private JTextArea inputArea;
	private JButton sendBtn;

	public AiAssistantPanel(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
		initUI();
		if (!mainWindow.getSettings().getAiSettings().isEnabled()) {
			chatArea.setText(NLS.str("ai_assistant.welcome_not_enabled") + "\n");
		}
	}

	private void initUI() {
		chatArea = new JTextArea();
		chatArea.setEditable(false);
		chatArea.setLineWrap(true);
		chatArea.setWrapStyleWord(true);
		chatArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		JScrollPane chatScroll = new JScrollPane(chatArea);
		chatScroll.setPreferredSize(new Dimension(500, 400));

		inputArea = new JTextArea(4, 40);
		inputArea.setLineWrap(true);
		inputArea.setWrapStyleWord(true);
		inputArea.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER && !e.isShiftDown()) {
					e.consume();
					send();
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}
		});
		JScrollPane inputScroll = new JScrollPane(inputArea);

		sendBtn = new JButton(NLS.str("ai_assistant.send"));
		sendBtn.addActionListener(ev -> send());
		JButton clearBtn = new JButton(NLS.str("ai_assistant.clear"));
		clearBtn.addActionListener(ev -> clear());
		JButton connectBtn = new JButton(NLS.str("ai_assistant.connect_provider"));
		connectBtn.addActionListener(ev -> mainWindow.openSettings(NLS.str("preferences.ai")));

		JPanel buttonsPanel = new JPanel();
		buttonsPanel.add(connectBtn);
		buttonsPanel.add(clearBtn);
		buttonsPanel.add(sendBtn);

		JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
		bottomPanel.add(inputScroll, BorderLayout.CENTER);
		bottomPanel.add(buttonsPanel, BorderLayout.PAGE_END);

		setLayout(new BorderLayout(5, 5));
		add(chatScroll, BorderLayout.CENTER);
		add(bottomPanel, BorderLayout.PAGE_END);
	}

	public void clear() {
		history.clear();
		chatArea.setText("");
	}

	/**
	 * Pre-fills the input with a question about the given code snippet and sends it right away.
	 */
	public void askAboutCode(String question, String code) {
		String fullQuestion = question + "\n\n```\n" + code + "\n```";
		inputArea.setText(fullQuestion);
		send();
	}

	private void send() {
		String text = inputArea.getText().trim();
		if (text.isEmpty()) {
			return;
		}
		AiSettings settings = mainWindow.getSettings().getAiSettings();
		if (!settings.isEnabled()) {
			JOptionPane.showMessageDialog(this, NLS.str("ai_assistant.not_enabled"),
					NLS.str("preferences.ai"), JOptionPane.WARNING_MESSAGE);
			return;
		}
		inputArea.setText("");
		appendLine(NLS.str("ai_assistant.you") + ":\n" + text + "\n");
		history.add(new AiChatMessage(AiChatMessage.ROLE_USER, text));
		setBusy(true);

		AtomicReference<String> resultText = new AtomicReference<>();
		AtomicReference<Boolean> success = new AtomicReference<>(false);
		mainWindow.getBackgroundExecutor().execute(NLS.str("ai_assistant.thinking"), () -> {
			try {
				AiClient client = new AiClient(settings);
				List<AiChatMessage> request = new ArrayList<>();
				request.add(new AiChatMessage(AiChatMessage.ROLE_SYSTEM,
						"You are an assistant embedded in the jadx Android decompiler GUI, "
								+ "helping the user understand decompiled Java code. Be concise."));
				request.addAll(history);
				String reply = client.sendMessage(request);
				resultText.set(reply);
				success.set(true);
			} catch (Exception e) {
				resultText.set(e.getMessage() != null ? e.getMessage() : e.toString());
				success.set(false);
			}
		}, status -> {
			setBusy(false);
			if (Boolean.TRUE.equals(success.get())) {
				String reply = resultText.get();
				history.add(new AiChatMessage(AiChatMessage.ROLE_ASSISTANT, reply));
				appendLine(NLS.str("ai_assistant.assistant") + ":\n" + reply + "\n");
			} else {
				appendLine(NLS.str("ai_assistant.error") + ": " + resultText.get() + "\n");
			}
		});
	}

	private void setBusy(boolean busy) {
		sendBtn.setEnabled(!busy);
		inputArea.setEnabled(!busy);
	}

	private void appendLine(String line) {
		chatArea.append(line + "\n");
		SwingUtilities.invokeLater(() -> {
			JScrollBar bar = ((JScrollPane) chatArea.getParent().getParent()).getVerticalScrollBar();
			bar.setValue(bar.getMaximum());
		});
	}
}
