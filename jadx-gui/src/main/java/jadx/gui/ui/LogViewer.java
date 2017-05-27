package jadx.gui.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import ch.qos.logback.classic.Level;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import jadx.gui.settings.JadxSettings;
import jadx.gui.utils.LogCollector;
import jadx.gui.utils.NLS;

class LogViewer extends JDialog {
	private static final long serialVersionUID = -2188700277429054641L;
	private static final Level[] LEVEL_ITEMS = {Level.DEBUG, Level.INFO, Level.WARN, Level.ERROR};

	private static Level level = Level.WARN;

	private final transient JadxSettings settings;
	private transient RSyntaxTextArea textPane;

	public LogViewer(JadxSettings settings) {
		this.settings = settings;
		initUI();
		registerLogListener();
		settings.loadWindowPos(this);
	}

	public final void initUI() {
		textPane = new RSyntaxTextArea();
		textPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		JPanel controlPane = new JPanel();
		controlPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		final JComboBox<Level> cb = new JComboBox<>(LEVEL_ITEMS);
		cb.setSelectedItem(level);
		cb.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int i = cb.getSelectedIndex();
				level = LEVEL_ITEMS[i];
				registerLogListener();
			}
		});
		JLabel levelLabel = new JLabel(NLS.str("log.level"));
		levelLabel.setLabelFor(cb);
		controlPane.add(levelLabel);
		controlPane.add(cb);

		JScrollPane scrollPane = new JScrollPane(textPane);

		JButton close = new JButton(NLS.str("tabs.close"));
		close.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				close();
			}
		});
		close.setAlignmentX(0.5f);

		Container contentPane = getContentPane();
		contentPane.add(controlPane, BorderLayout.PAGE_START);
		contentPane.add(scrollPane, BorderLayout.CENTER);
		contentPane.add(close, BorderLayout.PAGE_END);

		setTitle("Log Viewer");
		pack();
		setSize(800, 600);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setModalityType(ModalityType.MODELESS);
		setLocationRelativeTo(null);
	}

	private void registerLogListener() {
		LogCollector logCollector = LogCollector.getInstance();
		logCollector.resetListener();
		textPane.setText("");
		logCollector.registerListener(new LogCollector.ILogListener() {
			@Override
			public Level getFilterLevel() {
				return level;
			}

			@Override
			public void onAppend(final String logStr) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						textPane.append(logStr);
						textPane.updateUI();
					}
				});
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
