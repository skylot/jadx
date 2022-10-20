package jadx.gui.plugins.quark;

import java.awt.BorderLayout;
import java.awt.Container;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.settings.JadxSettings;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.ui.NodeLabel;

public class QuarkDialog extends JDialog {
	private static final long serialVersionUID = 4855753773520368215L;

	private static final Logger LOG = LoggerFactory.getLogger(QuarkDialog.class);

	private final transient JadxSettings settings;
	private final transient MainWindow mainWindow;
	private final List<Path> files;

	private JComboBox<Path> fileSelectCombo;

	public QuarkDialog(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
		this.settings = mainWindow.getSettings();
		this.files = filterOpenFiles(mainWindow);
		if (files.isEmpty()) {
			UiUtils.errorMessage(mainWindow, "Quark is unable to analyze loaded files");
			LOG.error("Quark: The files cannot be analyzed: {}", mainWindow.getProject().getFilePaths());
			return;
		}
		initUI();
	}

	private List<Path> filterOpenFiles(MainWindow mainWindow) {
		PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**.{apk,dex}");
		return mainWindow.getProject().getFilePaths()
				.stream()
				.filter(matcher::matches)
				.collect(Collectors.toList());
	}

	private void initUI() {
		JLabel description = new JLabel("Analyzing apk using Quark-Engine");
		JLabel selectApkText = new JLabel("Select Apk/Dex");
		description.setAlignmentX(0.5f);

		fileSelectCombo = new JComboBox<>(files.toArray(new Path[0]));
		fileSelectCombo.setRenderer((list, value, index, isSelected, cellHasFocus) -> new NodeLabel(value.getFileName().toString()));

		JPanel textPane = new JPanel();
		textPane.add(description);

		JPanel selectApkPanel = new JPanel();
		selectApkPanel.add(selectApkText);
		selectApkPanel.add(fileSelectCombo);

		JPanel buttonPane = new JPanel();
		JButton start = new JButton("Start");
		JButton close = new JButton("Close");
		close.addActionListener(event -> close());
		start.addActionListener(event -> startQuarkTasks());
		buttonPane.add(start);
		buttonPane.add(close);
		getRootPane().setDefaultButton(close);

		JPanel centerPane = new JPanel();
		centerPane.add(selectApkPanel);
		Container contentPane = getContentPane();

		contentPane.add(textPane, BorderLayout.PAGE_START);
		contentPane.add(centerPane);
		contentPane.add(buttonPane, BorderLayout.PAGE_END);

		setTitle("Quark Engine");
		pack();
		if (!mainWindow.getSettings().loadWindowPos(this)) {
			setSize(300, 140);
		}
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setModalityType(ModalityType.APPLICATION_MODAL);
		UiUtils.addEscapeShortCutToDispose(this);
	}

	private void startQuarkTasks() {
		Path apkFile = (Path) fileSelectCombo.getSelectedItem();
		new QuarkManager(mainWindow, apkFile).start();
		close();
	}

	private void close() {
		dispose();
	}

	@Override
	public void dispose() {
		settings.saveWindowPos(this);
		super.dispose();
	}
}
