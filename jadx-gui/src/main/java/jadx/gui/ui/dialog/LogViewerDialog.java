package jadx.gui.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import jadx.gui.logs.LogOptions;
import jadx.gui.logs.LogPanel;
import jadx.gui.settings.JadxSettings;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

public class LogViewerDialog extends JFrame {
	private static final long serialVersionUID = -2188700277429054641L;

	private static LogViewerDialog openLogDialog;

	private final transient JadxSettings settings;
	private final transient LogPanel logPanel;

	public static void open(MainWindow mainWindow, LogOptions logOptions) {
		LogViewerDialog logDialog;
		if (openLogDialog != null) {
			logDialog = openLogDialog;
		} else {
			logDialog = new LogViewerDialog(mainWindow, logOptions);
			openLogDialog = logDialog;
		}
		logDialog.setVisible(true);
		logDialog.toFront();
	}

	private LogViewerDialog(MainWindow mainWindow, LogOptions logOptions) {
		settings = mainWindow.getSettings();
		UiUtils.setWindowIcons(this);

		Runnable dock = () -> {
			mainWindow.getSettings().setDockLogViewer(true);
			dispose();
			mainWindow.showLogViewer(LogOptions.current());
		};
		logPanel = new LogPanel(mainWindow, logOptions, dock, this::dispose);
		Container contentPane = getContentPane();
		contentPane.add(logPanel, BorderLayout.CENTER);

		setTitle(NLS.str("log_viewer.title"));
		pack();
		setSize(800, 600);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLocationRelativeTo(null);
		settings.loadWindowPos(this);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				openLogDialog = null;
			}
		});
	}

	@Override
	public void dispose() {
		logPanel.dispose();
		settings.saveWindowPos(this);
		super.dispose();
	}
}
