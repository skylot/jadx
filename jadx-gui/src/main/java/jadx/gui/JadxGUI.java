package jadx.gui;

import javax.swing.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.settings.JadxSettings;
import jadx.gui.settings.JadxSettingsAdapter;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;
import jadx.gui.utils.logs.LogCollector;

public class JadxGUI {
	private static final Logger LOG = LoggerFactory.getLogger(JadxGUI.class);

	public static void main(String[] args) {
		try {
			LogCollector.register();
			final JadxSettings settings = JadxSettingsAdapter.load();
			// overwrite loaded settings by command line arguments
			if (!settings.overrideProvided(args)) {
				return;
			}
			if (!tryDefaultLookAndFeel()) {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
			NLS.setLocale(settings.getLangLocale());
			SwingUtilities.invokeLater(new MainWindow(settings)::open);
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
}

