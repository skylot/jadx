package jadx.gui.logs;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeListener;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.jetbrains.annotations.Nullable;

import ch.qos.logback.classic.Level;

import jadx.gui.settings.JadxSettings;
import jadx.gui.treemodel.JInputScript;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.AbstractCodeArea;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.utils.NLS;

public class LogPanel extends JPanel {
	private static final long serialVersionUID = -8077649118322056081L;

	private static final Level[] LEVEL_ITEMS = { Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR };

	private final MainWindow mainWindow;
	private final Runnable dockAction;
	private final Runnable hideAction;

	private RSyntaxTextArea textPane;
	private JComboBox<LogMode> modeCb;
	private JComboBox<Level> levelCb;

	private ChangeListener activeTabListener;

	public LogPanel(MainWindow mainWindow, LogOptions logOptions, Runnable dockAction, Runnable hideAction) {
		this.mainWindow = mainWindow;
		this.dockAction = dockAction;
		this.hideAction = hideAction;
		initUI(logOptions);
		applyLogOptions(logOptions);
	}

	public void applyLogOptions(LogOptions logOptions) {
		if (logOptions.getMode() == LogMode.CURRENT_SCRIPT) {
			String scriptName = getCurrentScriptName();
			if (scriptName != null) {
				logOptions = LogOptions.forScript(scriptName);
			}
			registerActiveTabListener();
		} else {
			removeActiveTabListener();
		}
		if (modeCb.getSelectedItem() != logOptions.getMode()) {
			modeCb.setSelectedItem(logOptions.getMode());
		}
		if (levelCb.getSelectedItem() != logOptions.getLogLevel()) {
			levelCb.setSelectedItem(logOptions.getLogLevel());
		}
		registerLogListener(logOptions);
	}

	public void loadSettings() {
		AbstractCodeArea.loadCommonSettings(mainWindow, textPane);
	}

	private void initUI(LogOptions logOptions) {
		JadxSettings settings = mainWindow.getSettings();
		textPane = AbstractCodeArea.getDefaultArea(mainWindow);
		textPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		modeCb = new JComboBox<>(LogMode.values());
		modeCb.setSelectedItem(logOptions.getMode());
		modeCb.addActionListener(e -> applyLogOptions(LogOptions.forMode((LogMode) modeCb.getSelectedItem())));
		JLabel modeLabel = new JLabel(NLS.str("log_viewer.mode"));
		modeLabel.setLabelFor(modeCb);

		levelCb = new JComboBox<>(LEVEL_ITEMS);
		levelCb.setSelectedItem(logOptions.getLogLevel());
		levelCb.addActionListener(e -> applyLogOptions(LogOptions.forLevel((Level) levelCb.getSelectedItem())));
		JLabel levelLabel = new JLabel(NLS.str("log_viewer.log_level"));
		levelLabel.setLabelFor(levelCb);

		JButton clearBtn = new JButton(NLS.str("log_viewer.clear"));
		clearBtn.addActionListener(ev -> {
			LogCollector.getInstance().reset();
			textPane.setText("");
		});

		JButton dockBtn = new JButton(NLS.str(settings.isDockLogViewer() ? "log_viewer.undock" : "log_viewer.dock"));
		dockBtn.addActionListener(ev -> dockAction.run());

		JButton hideBtn = new JButton(NLS.str("log_viewer.hide"));
		hideBtn.addActionListener(ev -> hideAction.run());

		JPanel start = new JPanel();
		start.setLayout(new BoxLayout(start, BoxLayout.LINE_AXIS));
		start.add(modeLabel);
		start.add(Box.createRigidArea(new Dimension(5, 0)));
		start.add(modeCb);
		start.add(Box.createRigidArea(new Dimension(15, 0)));
		start.add(levelLabel);
		start.add(Box.createRigidArea(new Dimension(5, 0)));
		start.add(levelCb);
		start.add(Box.createRigidArea(new Dimension(5, 0)));

		JPanel end = new JPanel();
		end.setLayout(new BoxLayout(end, BoxLayout.LINE_AXIS));
		end.add(clearBtn);
		end.add(Box.createRigidArea(new Dimension(15, 0)));
		end.add(dockBtn);
		end.add(Box.createRigidArea(new Dimension(15, 0)));
		end.add(hideBtn);

		JPanel controlPane = new JPanel();
		controlPane.setLayout(new BorderLayout());
		controlPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		controlPane.add(start, BorderLayout.LINE_START);
		controlPane.add(end, BorderLayout.LINE_END);

		JScrollPane scrollPane = new JScrollPane(textPane);

		setLayout(new BorderLayout(5, 5));
		add(controlPane, BorderLayout.PAGE_START);
		add(scrollPane, BorderLayout.CENTER);
	}

	private void registerLogListener(LogOptions logOptions) {
		LogCollector logCollector = LogCollector.getInstance();
		logCollector.removeListenerByClass(LogAppender.class);
		textPane.setText("");
		logCollector.registerListener(new LogAppender(logOptions, textPane));
	}

	private @Nullable String getCurrentScriptName() {
		ContentPanel selectedCodePanel = mainWindow.getTabbedPane().getSelectedCodePanel();
		if (selectedCodePanel != null) {
			JNode node = selectedCodePanel.getNode();
			if (node instanceof JInputScript) {
				return node.getName();
			}
		}
		return null;
	}

	private synchronized void registerActiveTabListener() {
		removeActiveTabListener();
		activeTabListener = e -> {
			String scriptName = getCurrentScriptName();
			if (scriptName != null) {
				applyLogOptions(LogOptions.forScript(scriptName));
			}
		};
		mainWindow.getTabbedPane().addChangeListener(activeTabListener);
	}

	private synchronized void removeActiveTabListener() {
		if (activeTabListener != null) {
			mainWindow.getTabbedPane().removeChangeListener(activeTabListener);
			activeTabListener = null;
		}
	}

	public void dispose() {
		LogCollector.getInstance().removeListenerByClass(LogAppender.class);
		removeActiveTabListener();
	}
}
