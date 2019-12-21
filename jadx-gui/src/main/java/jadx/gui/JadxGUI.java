package jadx.gui;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.cli.LogHelper;
import jadx.gui.settings.JadxSettings;
import jadx.gui.settings.JadxSettingsAdapter;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;
import jadx.gui.utils.SystemInfo;
import jadx.gui.utils.logs.LogCollector;

public class JadxGUI {
	private static final Logger LOG = LoggerFactory.getLogger(JadxGUI.class);

	public static void main(String[] args) {
		try {
			LogCollector.register();
			final JadxSettings settings = JadxSettingsAdapter.load();
			settings.setLogLevel(LogHelper.LogLevelEnum.INFO);
			// overwrite loaded settings by command line arguments
			if (!settings.overrideProvided(args)) {
				return;
			}
			if (!tryDefaultLookAndFeel()) {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
			NLS.setLocale(settings.getLangLocale());
			printSystemInfo();

			SwingUtilities.invokeLater(new MainWindow(settings)::init);
		} catch (Exception e) {
			LOG.error("Error: {}", e.getMessage(), e);
			System.exit(1);
		}
	}

	private static boolean tryDefaultLookAndFeel() {
		String defLaf = System.getProperty("swing.defaultlaf");
		if (defLaf != null) {
			try {
				UIManager.setLookAndFeel(defLaf);
				return true;
			} catch (Exception e) {
				LOG.error("Failed to set default laf: {}", defLaf, e);
			}
		}
		return false;
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
