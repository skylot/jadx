package jadx.gui.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import jadx.gui.jobs.IBackgroundTask;
import jadx.gui.jobs.TaskStatus;
import jadx.gui.settings.JadxSettings;
import jadx.gui.treemodel.JRoot;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

class QuarkDialog extends JDialog {

	private static final long serialVersionUID = 4855753773520368215L;

	private static final Logger LOG = LoggerFactory.getLogger(QuarkDialog.class);
	private static final String QUARK_CMD_LOG_MESSAGE = "Running Quark cmd: {}";
	private static final String QUARK_INTERRUPT_MESSAGE = "Quark process interrupted: {}";
	private static final String QUARK_FAILED_MESSAGE = "Failed to execute Quark.";
	private static final String QUARK_CMD = "quark";
	private static final int LARGE_APK_SIZE = 30;
	private static final Path QUARK_DIR_PATH = Paths.get(System.getProperty("user.home"), ".quark-engine");

	private Path venvPath = Paths.get(QUARK_DIR_PATH.toString(), "quark_venv");
	private File quarkReportFile;

	private final transient JadxSettings settings;
	private final transient MainWindow mainWindow;

	private JComboBox<String> fileSelectCombo;

	private final List<Path> files;
	private Map<String, Path> choosableFiles = new HashMap<>();

	public QuarkDialog(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
		this.settings = mainWindow.getSettings();
		this.files = mainWindow.getWrapper().getOpenPaths();
		fileNameExtensionFilter();
		if (choosableFiles.isEmpty()) {
			UiUtils.errorMessage(mainWindow, "Quark is unable to analyze the selected file.");
			LOG.error("Quark: The files cannot be analyze. {}", files);
			return;
		}
		initUI();
	}

	private void fileNameExtensionFilter() {
		String[] extensions = new String[] { "apk", "dex" };

		for (Path filePath : this.files) {
			String fileName = filePath.toString();
			int dotIndex = fileName.lastIndexOf('.');
			String extension = (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);

			if (Arrays.stream(extensions).noneMatch(extension::equals)) {
				LOG.debug("Quark: {} is not apk nor dex", fileName);
				continue;
			}
			choosableFiles.put(fileName, filePath);
		}
	}

	public final void initUI() {
		JLabel description = new JLabel("Analyzing apk using Quark-Engine");
		JLabel selectApkText = new JLabel("Select Apk/Dex");
		description.setAlignmentX(0.5f);

		String[] comboFiles = choosableFiles.keySet().toArray(new String[choosableFiles.size()]);
		fileSelectCombo = new JComboBox<>(comboFiles);

		JPanel textPane = new JPanel();

		textPane.add(description);

		JPanel selectApkPanel = new JPanel();
		selectApkPanel.add(selectApkText);
		selectApkPanel.add(fileSelectCombo);

		JPanel buttonPane = new JPanel();
		JButton start = new JButton("Start");
		JButton close = new JButton(NLS.str("tabs.close"));
		close.addActionListener(event -> close());
		start.addActionListener(event -> mainWindow.getBackgroundExecutor().execute(new QuarkTask()));
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
		setModalityType(ModalityType.MODELESS);
		UiUtils.addEscapeShortCutToDispose(this);
	}

	private void close() {
		dispose();
	}

	@Override
	public void dispose() {
		settings.saveWindowPos(this);
		super.dispose();
	}

	private class QuarkTask implements IBackgroundTask {

		private Process quarkProcess;
		private boolean isVenv = false;

		public QuarkTask() {
			dispose();
		}

		private boolean isPipInstalled() {
			List<String> cmdList = new ArrayList<>();
			cmdList.add("pip3");
			return executeCommand(cmdList);
		}

		private boolean isQuarkInstalled() {
			List<String> cmdList = new ArrayList<>();
			cmdList.add(QUARK_CMD);
			if (executeCommand(cmdList)) {
				return true;
			}

			isVenv = true;
			cmdList = new ArrayList<>();
			cmdList.add(getVenvPath(QUARK_CMD).toString());
			return executeCommand(cmdList);
		}

		private void createVirtualenv() {

			// Check if venv exist
			if (Files.exists(getVenvPath("activate"))) {
				return;
			}

			List<String> cmdList = new ArrayList<>();

			if (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0) {
				cmdList.add("python");
				cmdList.add("-m");
				cmdList.add("venv");
			} else {
				cmdList.add("virtualenv");
			}

			cmdList.add(venvPath.toString());
			try {
				LOG.debug(QUARK_CMD_LOG_MESSAGE, cmdList);
				Process process = Runtime.getRuntime().exec(cmdList.toArray(new String[0]));
				process.waitFor();
			} catch (InterruptedException e) {
				LOG.error(QUARK_INTERRUPT_MESSAGE, e.getMessage(), e);
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				UiUtils.errorMessage(mainWindow, "Failed to create virtual environment.");
				LOG.error("Failed to create virtual environment: {}", e.getMessage(), e);
			}
		}

		private boolean installQuark() {
			List<String> cmdList = new ArrayList<>();
			String command = (isVenv) ? getVenvPath("pip3").toString() : "pip3";
			cmdList.add(command);
			cmdList.add("install");
			cmdList.add("quark-engine");
			cmdList.add("--upgrade");
			try {
				LOG.debug(QUARK_CMD_LOG_MESSAGE, cmdList);
				Process process = Runtime.getRuntime().exec(cmdList.toArray(new String[0]));
				process.waitFor();

				if (!isQuarkInstalled()) {
					return false;
				}
			} catch (InterruptedException e) {
				LOG.error(QUARK_INTERRUPT_MESSAGE, e.getMessage(), e);
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				UiUtils.errorMessage(mainWindow, "Failed to install quark-engine.");
				LOG.error("Failed to execute pip install command: {}", String.join(" ", cmdList), e);
				return false;
			}
			return true;
		}

		private void updateQuarkRules() {
			List<String> cmdList = new ArrayList<>();
			String command = (isVenv) ? getVenvPath("freshquark").toString() : "freshquark";
			cmdList.add(command);
			executeCommand(cmdList);
		}

		private boolean analyzeAPK() {
			try {
				updateQuarkRules();
				quarkReportFile = File.createTempFile("QuarkReport-", ".json");
				String apkName = (String) fileSelectCombo.getSelectedItem();
				String apkPath = choosableFiles.get(apkName).toString();

				List<String> cmdList = new ArrayList<>();
				String command = (isVenv) ? getVenvPath(QUARK_CMD).toString() : QUARK_CMD;
				cmdList.add(command);
				cmdList.add("-a");
				cmdList.add(apkPath);
				cmdList.add("-o");
				cmdList.add(quarkReportFile.getAbsolutePath());
				LOG.debug(QUARK_CMD_LOG_MESSAGE, cmdList);
				quarkProcess = Runtime.getRuntime().exec(cmdList.toArray(new String[0]));

				try (BufferedReader buf = new BufferedReader(new InputStreamReader(quarkProcess.getInputStream()))) {
					String output = null;
					while ((output = buf.readLine()) != null) {
						LOG.debug(output);
					}
				}
			} catch (Exception e) {
				LOG.error("Failed to execute Quark: {}", e.getMessage(), e);
				return false;
			}
			return true;
		}

		private boolean executeCommand(List<String> cmdList) {
			try {
				LOG.debug(QUARK_CMD_LOG_MESSAGE, cmdList);
				Process process = Runtime.getRuntime().exec(cmdList.toArray(new String[0]));
				process.waitFor();
			} catch (InterruptedException e) {
				LOG.error(QUARK_INTERRUPT_MESSAGE, e.getMessage(), e);
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				LOG.error("Failed to execute command: {}", String.join(" ", cmdList), e);
				return false;
			}
			return true;
		}

		public boolean checkFileSize(int sizeThreshold) {
			String apkName = (String) fileSelectCombo.getSelectedItem();

			try {
				int fileSize = (int) Files.size(choosableFiles.get(apkName)) / 1024 / 1024;
				if (fileSize > sizeThreshold) {
					return false;
				}
			} catch (Exception e) {
				LOG.error("Failed to calculate file: {}", e.getMessage(), e);
				return false;
			}
			return true;
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
				UiUtils.errorMessage(mainWindow, "Failed to load Quark report.");
				LOG.error("Failed to load Quark report.", e);
			}
		}

		private Path getVenvPath(String cmd) {
			String os = System.getProperty("os.name").toLowerCase();
			if (os.indexOf("win") >= 0) {
				return Paths.get(venvPath.toString(), "Scripts", String.format("%s.exe", cmd));
			} else {
				return Paths.get(venvPath.toString(), "bin", cmd);
			}
		}

		@Override
		public String getTitle() {
			return "Quark:";
		}

		@Override
		public boolean canBeCanceled() {
			return true;
		}

		@Override
		public List<Runnable> scheduleJobs() {
			List<Runnable> jobs = new ArrayList<>();

			// mkdir `$HOME/.quark-engine/`
			File directory = new File(QUARK_DIR_PATH.toString());
			if (!directory.isDirectory()) {
				directory.mkdirs();
			}

			if (!checkFileSize(LARGE_APK_SIZE)) {
				int result = JOptionPane.showConfirmDialog(mainWindow,
						"The selected file size is too large (over 30M) that may take a long time to analyze, do you want to continue",
						"Quark: Warning", JOptionPane.YES_NO_OPTION);
				if (result == JOptionPane.NO_OPTION) {
					return jobs;
				}
			}

			jobs.add(() -> {
				if (!isPipInstalled()) {
					UiUtils.errorMessage(mainWindow, "Pip is not installed.");
					LOG.error("Pip is not installed");
					mainWindow.cancelBackgroundJobs();
				}
			});

			jobs.add(() -> {
				mainWindow.getProgressPane().setLabel("Check Quark installed");
				if (!isQuarkInstalled()) {
					LOG.warn("Quark is not installed, do you want to install it from PyPI?");
					int result = JOptionPane.showConfirmDialog(mainWindow,
							"Quark is not installed, do you want to install it from PyPI?", "Warning",
							JOptionPane.YES_NO_OPTION);

					if (result == JOptionPane.YES_OPTION) {
						mainWindow.getProgressPane().setLabel("Installing Quark");
						createVirtualenv();
						if (!installQuark()) {
							UiUtils.errorMessage(mainWindow, "Failed to install quark-engine.");
							mainWindow.cancelBackgroundJobs();
						}
					}
					if (result == JOptionPane.NO_OPTION) {
						mainWindow.cancelBackgroundJobs();
					}
				}
			});

			jobs.add(() -> {
				mainWindow.getProgressPane().setLabel("Analyzing");
				if (!analyzeAPK()) {
					UiUtils.errorMessage(mainWindow, "Quark: Failed to analyze apk.");
					mainWindow.cancelBackgroundJobs();
				}
			});

			return jobs;
		}

		@Override
		public void onFinish(TaskStatus status, long skipped) {

			if (quarkProcess.exitValue() != 0) {
				LOG.error(QUARK_FAILED_MESSAGE);
				return;
			}
			loadReportFile();
		}
	}
}
