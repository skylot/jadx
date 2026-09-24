package jadx.gui.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import org.jetbrains.annotations.Nullable;

import jadx.gui.settings.JadxSettings;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.ai.AiAssistantPanel;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

/**
 * Standalone window hosting the {@link AiAssistantPanel}. Only one instance is kept open at a time.
 */
public class AiAssistantDialog extends JFrame {
	private static final long serialVersionUID = 1L;

	private static AiAssistantDialog openDialog;

	private final transient JadxSettings settings;
	private final transient AiAssistantPanel panel;

	public static AiAssistantDialog open(MainWindow mainWindow) {
		AiAssistantDialog dialog;
		if (openDialog != null) {
			dialog = openDialog;
		} else {
			dialog = new AiAssistantDialog(mainWindow);
			openDialog = dialog;
		}
		dialog.setVisible(true);
		dialog.toFront();
		return dialog;
	}

	public static @Nullable AiAssistantDialog getOpenDialog() {
		return openDialog;
	}

	private AiAssistantDialog(MainWindow mainWindow) {
		settings = mainWindow.getSettings();
		UiUtils.setWindowIcons(this);

		panel = new AiAssistantPanel(mainWindow);
		Container contentPane = getContentPane();
		contentPane.add(panel, BorderLayout.CENTER);

		setTitle(NLS.str("ai_assistant.title"));
		pack();
		setSize(700, 600);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLocationRelativeTo(null);
		settings.loadWindowPos(this);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				openDialog = null;
			}
		});
	}

	public AiAssistantPanel getPanel() {
		return panel;
	}

	@Override
	public void dispose() {
		settings.saveWindowPos(this);
		super.dispose();
	}
}
