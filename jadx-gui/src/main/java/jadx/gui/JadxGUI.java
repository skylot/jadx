package jadx.gui;

import jadx.gui.settings.JadxSettings;
import jadx.gui.settings.JadxSettingsAdapter;
import jadx.gui.ui.MainWindow;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JadxGUI {
	private static final Logger LOG = LoggerFactory.getLogger(JadxGUI.class);

	public static void main(String[] args) {
		try {
			final JadxSettings jadxArgs = JadxSettingsAdapter.load();
			// overwrite loaded settings by command line arguments
			if (!jadxArgs.processArgs(args)) {
				return;
			}
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					MainWindow window = new MainWindow(jadxArgs);
					window.open();
				}
			});
		} catch (Throwable e) {
			LOG.error("Error: {}", e.getMessage());
			System.exit(1);
		}
	}
}

