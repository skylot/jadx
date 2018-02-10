package jadx.gui;

import javax.swing.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.settings.JadxSettings;
import jadx.gui.settings.JadxSettingsAdapter;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.LogCollector;

public class JadxGUI {
	private static final Logger LOG = LoggerFactory.getLogger(JadxGUI.class);

	public static void main(String[] args) {
		try {
			LogCollector.register();
			final JadxSettings settings = JadxSettingsAdapter.load();
			// overwrite loaded settings by command line arguments
			if (!settings.processArgs(args)) {
				return;
			}
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			SwingUtilities.invokeLater(() -> {
				MainWindow window = new MainWindow(settings);
				window.open();
			});
		} catch (Exception e) {
			LOG.error("Error: {}", e.getMessage(), e);
			System.exit(1);
		}
	}
}

