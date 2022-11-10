package jadx.gui;

import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.cli.LogHelper;
import jadx.gui.logs.LogCollector;
import jadx.gui.settings.JadxSettings;
import jadx.gui.settings.JadxSettingsAdapter;
import jadx.gui.ui.ExceptionDialog;
import jadx.gui.ui.MainWindow;
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
			});
		} catch (Exception e) {
			LOG.error("Error: {}", e.getMessage(), e);
			System.exit(1);
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
