package jadx.gui.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import jadx.gui.settings.JadxSettings;
import jadx.gui.treemodel.JRoot;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

class QuarkDialog extends JDialog {

	private static final long serialVersionUID = 4855753773520368215L;

	private static final Logger LOG = LoggerFactory.getLogger(QuarkDialog.class);

	private File quarkReportFile;

	private final transient JadxSettings settings;
	private final transient MainWindow mainWindow;
	private JProgressBar progressBar;
	private JPanel progressPane;

	private JComboBox<String> selectFile;

	private final List<Path> files;
	private ArrayList<Path> analyzeFile = new ArrayList<Path>();

	public QuarkDialog(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
		this.settings = mainWindow.getSettings();
		this.files = mainWindow.getWrapper().getOpenPaths();

		if (!prepareAnalysis()) {
			// The files are unable to analysis by Quark
			return;
		}
		initUI();
	}

	private boolean prepareAnalysis() {
		String[] exts = new String[] { "apk", "dex" };

		if (this.files.size() != 1) {
			for (Path filePath : this.files) {
				String fileName = filePath.toString();
				int dotIndex = fileName.lastIndexOf('.');
				String extension = (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);

				if (Arrays.stream(exts).noneMatch(extension::equals)) {
					LOG.warn("Quark: Current file can't be analysis: {}", fileName);
					continue;
				}
				analyzeFile.add(filePath);
			}
			return true;
		}
		String fileName = this.files.get(0).toString();
		int dotIndex = fileName.lastIndexOf('.');
		String extension = (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
		if (Arrays.stream(exts).noneMatch(extension::equals)) {
			LOG.warn("Quark: Current file can't be analysis: {}", fileName);
			return false;
		}
		analyzeFile.add(this.files.get(0));
		return true;
	}

	private String[] filesToStringArr() {
		String[] arr = new String[files.size()];
		int index = 0;
		for (Path file : analyzeFile) {
			arr[index] = file.getFileName().toString();
			index++;
		}
		return arr;
	}

	public final void initUI() {
		JLabel description = new JLabel("Analyzing apk using Quark-Engine");
		JLabel selectApkText = new JLabel("Select Apk/Dex");
		description.setAlignmentX(0.5f);

		selectFile = new JComboBox<String>(filesToStringArr());

		JPanel textPane = new JPanel();

		textPane.add(description);

		JPanel selectApkPanel = new JPanel();
		selectApkPanel.add(selectApkText);
		selectApkPanel.add(selectFile);

		progressPane = new JPanel();
		progressPane.setVisible(false);
		progressPane.setSize(150, 10);

		progressBar = new JProgressBar(0, 100);
		progressBar.setSize(150, 10);
		progressBar.setIndeterminate(true);
		progressBar.setStringPainted(false);
		progressPane.add(progressBar);

		JPanel buttonPane = new JPanel();
		JButton start = new JButton("Start");
		JButton close = new JButton(NLS.str("tabs.close"));
		close.addActionListener(event -> close());
		start.addActionListener(event -> analyzeAPK());
		buttonPane.add(start);
		buttonPane.add(close);
		getRootPane().setDefaultButton(close);

		JPanel centerPane = new JPanel();
		centerPane.add(selectApkPanel);
		centerPane.add(progressPane);
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
		setModalityType(ModalityType.MODELESS);
		UiUtils.addEscapeShortCutToDispose(this);
	}

	private void analyzeAPK() {
		LoadTask task = new LoadTask();
		task.execute();
	}

	private void loadReportFile() {
		try (Reader reader = new FileReader(quarkReportFile)) {
			JsonObject quarkReport = (JsonObject) JsonParser.parseReader(reader);
			QuarkReport quarkNode = QuarkReport.analysisAPK(quarkReport);

			JRoot root = mainWindow.getCacheObject().getJRoot();
			root.update();
			root.add(quarkNode);
			mainWindow.reloadTree();
		} catch (Exception e) {
			LOG.error("Quark: Load report failed: ", e);
		}
	}

	private void close() {
		dispose();
	}

	@Override
	public void dispose() {
		settings.saveWindowPos(this);
		super.dispose();
	}

	private class LoadTask extends SwingWorker<Void, Void> {
		public LoadTask() {
			progressPane.setVisible(true);
		}

		@Override
		public Void doInBackground() {
			try {
				quarkReportFile = File.createTempFile("QuarkReport-", ".json");

				String apkName = (String) selectFile.getSelectedItem();
				String apkPath = null;
				for (Path path : files) {
					if (path.getFileName().toString().equals(apkName)) {
						apkPath = path.toString();
					}
				}
				List<String> cmdList = new ArrayList<>();
				cmdList.add("quark");
				cmdList.add("-a");
				cmdList.add(apkPath);
				cmdList.add("-s");
				cmdList.add("-o");
				cmdList.add(quarkReportFile.getAbsolutePath());
				LOG.debug("Running Quark cmd: {}", String.join(" ", cmdList));
				Process process = Runtime.getRuntime().exec(cmdList.toArray(new String[0]));
				try (BufferedReader buf = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
					LOG.debug("Quark analyzing...");
					while (process.isAlive()) {
						String output = buf.readLine();
						if (output != null) {
							LOG.debug(output);
						}
					}
				}
			} catch (Exception e) {
				LOG.error("Quark failed: ", e);
				dispose();
			}
			return null;
		}

		@Override
		public void done() {
			loadReportFile();
			dispose();
		}
	}
}
