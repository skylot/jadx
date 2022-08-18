package jadx.gui.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import ch.qos.logback.classic.Level;

import jadx.gui.settings.JadxSettings;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.AbstractCodeArea;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.logs.ILogListener;
import jadx.gui.utils.logs.LogCollector;

public class LogViewerDialog extends JFrame {
	private static final long serialVersionUID = -2188700277429054641L;
	private static final Level[] LEVEL_ITEMS = { Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR };

	private static Level level = Level.WARN;

	private final transient JadxSettings settings;
	private transient RSyntaxTextArea textPane;
	private JComboBox<Level> levelCb;

	private static LogViewerDialog openLogDialog;

	public static void open(MainWindow mainWindow) {
		openWithLevel(mainWindow, level);
	}

	public static void openWithLevel(MainWindow mainWindow, Level newLevel) {
		level = newLevel;
		if (openLogDialog == null) {
			LogViewerDialog newLogDialog = new LogViewerDialog(mainWindow);
			newLogDialog.setVisible(true);
			openLogDialog = newLogDialog;
		} else {
			LogViewerDialog logDialog = openLogDialog;
			logDialog.levelCb.setSelectedItem(level);
			logDialog.setVisible(true);
			logDialog.toFront();
		}
	}

	private LogViewerDialog(MainWindow mainWindow) {
		this.settings = mainWindow.getSettings();
		initUI(mainWindow);
		registerLogListener();
		settings.loadWindowPos(this);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				openLogDialog = null;
			}
		});
	}

	public final void initUI(MainWindow mainWindow) {
		UiUtils.setWindowIcons(this);

		textPane = AbstractCodeArea.getDefaultArea(mainWindow);
		textPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		JPanel controlPane = new JPanel();
		controlPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		levelCb = new JComboBox<>(LEVEL_ITEMS);
		levelCb.setSelectedItem(level);
		levelCb.addActionListener(e -> {
			int i = levelCb.getSelectedIndex();
			level = LEVEL_ITEMS[i];
			registerLogListener();
		});
		JLabel levelLabel = new JLabel(NLS.str("log_viewer.log_level"));
		levelLabel.setLabelFor(levelCb);
		controlPane.add(levelLabel);
		controlPane.add(levelCb);

		JScrollPane scrollPane = new JScrollPane(textPane);

		JButton close = new JButton(NLS.str("tabs.close"));
		close.addActionListener(event -> close());
		close.setAlignmentX(0.5f);

		Container contentPane = getContentPane();
		contentPane.add(controlPane, BorderLayout.PAGE_START);
		contentPane.add(scrollPane, BorderLayout.CENTER);
		contentPane.add(close, BorderLayout.PAGE_END);

		setTitle(NLS.str("log_viewer.title"));
		pack();
		setSize(800, 600);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLocationRelativeTo(null);
	}

	private void registerLogListener() {
		LogCollector logCollector = LogCollector.getInstance();
		logCollector.resetListener();
		textPane.setText("");
		logCollector.registerListener(new ILogListener() {
			@Override
			public Level getFilterLevel() {
				return level;
			}

			@Override
			public void onAppend(final String logStr) {
				SwingUtilities.invokeLater(() -> textPane.append(logStr));
			}
		});
	}

	private void close() {
		dispose();
	}

	@Override
	public void dispose() {
		LogCollector.getInstance().resetListener();
		settings.saveWindowPos(this);
		super.dispose();
	}
}
