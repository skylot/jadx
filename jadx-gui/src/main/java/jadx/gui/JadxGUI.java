package jadx.gui;

import java.awt.Desktop;

import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.cli.JadxCLIArgs;
import jadx.cli.config.JadxConfigAdapter;
import jadx.commons.app.JadxSystemInfo;
import jadx.core.Jadx;
import jadx.core.utils.JadxBuildInfo;
import jadx.core.utils.files.FileUtils;
import jadx.gui.logs.LogCollector;
import jadx.gui.settings.GuiConfigLocale;
import jadx.gui.settings.JadxSettings;
import jadx.gui.settings.JadxSettingsData;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.LafManager;

public class JadxGUI {
	private static final Logger LOG = LoggerFactory.getLogger(JadxGUI.class);

	public static void main(String[] args) {
		try {
			GuiConfigLocale.load();
			JadxConfigAdapter<JadxSettingsData> configAdapter = JadxSettings.buildConfigAdapter();
			JadxSettingsData settingsData = JadxCLIArgs.processArgs(args, new JadxSettingsData(), configAdapter);
			if (settingsData == null) {
				return;
			}
			JadxSettings settings = new JadxSettings(configAdapter);
			settings.loadSettingsData(settingsData);
			GuiConfigLocale.checkConfig(settingsData);

			LogCollector.register();
			printSystemInfo();
			SwingUtilities.invokeLater(() -> {
				LafManager.init(settings);
				settings.getFontSettings().updateDefaultFont();
				MainWindow mw = new MainWindow(settings);
				registerOpenFileHandler(mw);
				mw.init();
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
			LOG.debug("Starting jadx-gui: version: {}, bundle: {}. JVM: {} {}. OS: {}, version: {}, arch: {}",
					Jadx.getVersion(), JadxBuildInfo.getJadxBundleType(),
					JadxSystemInfo.JAVA_VM, JadxSystemInfo.JAVA_VER,
					JadxSystemInfo.OS_NAME, JadxSystemInfo.OS_VERSION, JadxSystemInfo.OS_ARCH);
		}
	}
}
