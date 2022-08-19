package jadx.gui.plugins.quark;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.jobs.BackgroundExecutor;
import jadx.gui.treemodel.JRoot;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.SystemInfo;
import jadx.gui.utils.UiUtils;

public class QuarkManager {
	private static final Logger LOG = LoggerFactory.getLogger(QuarkManager.class);

	private static final Path QUARK_DIR_PATH = Paths.get(System.getProperty("user.home"), ".quark-engine");
	private static final Path VENV_PATH = QUARK_DIR_PATH.resolve("quark_venv");
	private static final int LARGE_APK_SIZE = 30;

	private final MainWindow mainWindow;
	private final Path apkPath;

	private boolean useVEnv;
	private boolean installComplete;
	private Path reportFile;

	public QuarkManager(MainWindow mainWindow, Path apkPath) {
		this.mainWindow = mainWindow;
		this.apkPath = apkPath;
	}

	public void start() {
		if (!checkFileSize(LARGE_APK_SIZE)) {
			int result = JOptionPane.showConfirmDialog(mainWindow,
					"The selected file size is too large (over 30M) that may take a long time to analyze, do you want to continue",
					"Quark: Warning", JOptionPane.YES_NO_OPTION);
			if (result == JOptionPane.NO_OPTION) {
				return;
			}
		}
		BackgroundExecutor executor = mainWindow.getBackgroundExecutor();
		executor.execute("Quark install", this::checkInstall,
				installStatus -> executor.execute("Quark analysis", this::startAnalysis, analysisStatus -> loadReport()));
	}

	private void checkInstall() {
		try {
			if (checkCommand("quark")) {
				useVEnv = false;
				installComplete = true;
				return;
			}
			useVEnv = true;
			if (checkVEnvCommand("quark")) {
				installComplete = true;
				installQuark(); // upgrade quark
				return;
			}
			int result = JOptionPane.showConfirmDialog(mainWindow,
					"Quark is not installed, do you want to install it from PyPI?", "Warning",
					JOptionPane.YES_NO_OPTION);
			if (result == JOptionPane.NO_OPTION) {
				installComplete = false;
				return;
			}
			createVirtualEnv();
			installQuark();
			installComplete = true;
		} catch (Exception e) {
			UiUtils.errorMessage(mainWindow, e.getMessage());
			LOG.error("Failed to install quark", e);
			installComplete = false;
		}
	}

	private void startAnalysis() {
		if (!installComplete) {
			return;
		}
		try {
			updateQuarkRules();
			reportFile = Files.createTempFile("QuarkReport-", ".json").toAbsolutePath();
			List<String> cmd = new ArrayList<>();
			cmd.add(getCommand("quark"));
			cmd.add("-a");
			cmd.add(apkPath.toString());
			cmd.add("-o");
			cmd.add(reportFile.toString());
			runCommand(cmd);
		} catch (Exception e) {
			UiUtils.errorMessage(mainWindow, "Failed to execute Quark");
			LOG.error("Failed to execute Quark", e);
		}
	}

	private void loadReport() {
		try {
			QuarkReportNode quarkNode = new QuarkReportNode(reportFile);
			JRoot root = mainWindow.getTreeRoot();
			root.replaceCustomNode(quarkNode);
			root.update();
			mainWindow.reloadTree();
			mainWindow.getTabbedPane().showNode(quarkNode);
		} catch (Exception e) {
			UiUtils.errorMessage(mainWindow, "Failed to load Quark report.");
			LOG.error("Failed to load Quark report.", e);
		}
	}

	private void createVirtualEnv() {
		if (Files.exists(getVenvPath("activate"))) {
			return;
		}
		File directory = QUARK_DIR_PATH.toFile();
		if (!directory.isDirectory()) {
			if (!directory.mkdirs()) {
				throw new JadxRuntimeException("Failed create directory: " + directory);
			}
		}
		List<String> cmd = new ArrayList<>();
		if (SystemInfo.IS_WINDOWS) {
			cmd.add("python");
			cmd.add("-m");
			cmd.add("venv");
		} else {
			cmd.add("virtualenv");
		}
		cmd.add(VENV_PATH.toString());
		try {
			runCommand(cmd);
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to create virtual environment", e);
		}
	}

	private void installQuark() {
		List<String> cmd = new ArrayList<>();
		cmd.add(getCommand("pip3"));
		cmd.add("install");
		cmd.add("quark-engine");
		cmd.add("--upgrade");
		try {
			runCommand(cmd);
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to install quark-engine", e);
		}
	}

	private void updateQuarkRules() {
		List<String> cmd = new ArrayList<>();
		cmd.add(getCommand("freshquark"));
		try {
			runCommand(cmd);
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to update quark rules", e);
		}
	}

	public boolean checkFileSize(int sizeThreshold) {
		try {
			int fileSize = (int) Files.size(apkPath) / 1024 / 1024;
			if (fileSize > sizeThreshold) {
				return false;
			}
		} catch (Exception e) {
			LOG.error("Failed to calculate file: {}", e.getMessage(), e);
			return false;
		}
		return true;
	}

	private String getCommand(String cmd) {
		if (useVEnv) {
			return getVenvPath(cmd).toAbsolutePath().toString();
		}
		return cmd;
	}

	private boolean checkVEnvCommand(String cmd) {
		Path venvPath = getVenvPath(cmd);
		return checkCommand(venvPath.toAbsolutePath().toString());
	}

	private Path getVenvPath(String cmd) {
		if (SystemInfo.IS_WINDOWS) {
			return VENV_PATH.resolve("Scripts").resolve(cmd + ".exe");
		}
		return VENV_PATH.resolve("bin").resolve(cmd);
	}

	private void runCommand(List<String> cmd) throws Exception {
		LOG.debug("Running command: {}", String.join(" ", cmd));
		Process process = Runtime.getRuntime().exec(cmd.toArray(new String[0]));
		try (BufferedReader buf = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
			buf.lines().forEach(msg -> LOG.debug("# {}", msg));
		} finally {
			process.waitFor();
		}
	}

	private boolean checkCommand(String... cmd) {
		try {
			Process process = Runtime.getRuntime().exec(cmd);
			process.waitFor();
		} catch (Exception e) {
			return false;
		}
		return true;
	}
}
