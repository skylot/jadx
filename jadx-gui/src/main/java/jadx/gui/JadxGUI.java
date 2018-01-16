package jadx.gui;

import javax.swing.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.utils.exceptions.JadxException;
import jadx.gui.settings.JadxSettings;
import jadx.gui.settings.JadxSettingsAdapter;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.LogCollector;

public class JadxGUI {
	private static final Logger LOG = LoggerFactory.getLogger(JadxGUI.class);

	public static void main(String[] args) {
		try {
			LogCollector.register();
			final JadxSettings jadxArgs = JadxSettingsAdapter.load();
			// overwrite loaded settings by command line arguments
			if (!jadxArgs.processArgs(args)) {
				return;
			}
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					try {
						MainWindow window = new MainWindow(jadxArgs);
						window.open();
					}
					catch(JadxException e) {
						throw new RuntimeException(e);
					}
				}
			});
		} catch (Throwable e) {
			LOG.error("Error: {}", e.getMessage(), e);
			System.exit(1);
		}
	}
}

