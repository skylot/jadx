package jadx.gui;

import java.awt.Desktop;

import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.cli.LogHelper;
import jadx.core.utils.files.FileUtils;
import jadx.gui.logs.LogCollector;
import jadx.gui.settings.JadxSettings;
import jadx.gui.settings.JadxSettingsAdapter;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.dialog.ExceptionDialog;
import jadx.gui.utils.LafManager;
import jadx.gui.utils.NLS;
import jadx.gui.utils.SystemInfo;

public class JadxGUI {
	private static final Logger LOG = LoggerFactory.getLogger(JadxGUI.class);

	public static void main(String[] args) {
		try {
			LogCollector.register();
			JadxSettings settings = JadxSettingsAdapter.load();
			settings.setLogLevel(LogHelper.LogLevelEnum.INFO);
			// overwrite loaded settings by command line arguments
			if (!settings.overrideProvided(args)) {
				return;
			}
			LogHelper.initLogLevel(settings);
			LogHelper.setLogLevelsForDecompileStage();
			printSystemInfo();

			LafManager.init(settings);
			NLS.setLocale(settings.getLangLocale());
			ExceptionDialog.registerUncaughtExceptionHandler();
			SwingUtilities.invokeLater(() -> {
				MainWindow mw = new MainWindow(settings);
				mw.init();
				registerOpenFileHandler(mw);
			});
		} catch (Exception e) {
			LOG.error("Error: {}", e.getMessage(), e);
			System.exit(1);
		}
	}

	private static void registerOpenFileHandler(MainWindow mw) {
		try {
			if (Desktop.isDesktopSupported()) {
				Desktop desktop = Desktop.getDesktop();
				if (desktop.isSupported(Desktop.Action.APP_OPEN_FILE)) {
					desktop.setOpenFileHandler(e -> mw.open(FileUtils.toPaths(e.getFiles())));
				}
			}
		} catch (Throwable e) {
			LOG.error("Failed to register open file handler", e);
		}
	}

	private static void printSystemInfo() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Starting jadx-gui. Version: '{}'. JVM: {} {}. OS: {} {}",
					SystemInfo.JADX_VERSION,
					SystemInfo.JAVA_VM, SystemInfo.JAVA_VER,
					SystemInfo.OS_NAME, SystemInfo.OS_VERSION);
		}
	}
}
