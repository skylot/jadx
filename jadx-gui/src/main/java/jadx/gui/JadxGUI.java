package jadx.gui;

import jadx.cli.JadxCLIArgs;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JadxGUI {
	private static final Logger LOG = LoggerFactory.getLogger(JadxGUI.class);

	public static void main(String[] args) {
		try {
			final JadxCLIArgs jadxArgs = new JadxCLIArgs(args);
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					JadxWrapper wrapper = new JadxWrapper(jadxArgs);
					MainWindow mainWindow = new MainWindow(wrapper);
					mainWindow.setVisible(true);

					if (!jadxArgs.getInput().isEmpty()) {
						mainWindow.openFile(jadxArgs.getInput().get(0));
					}
				}
			});
		} catch (Throwable e) {
			LOG.error("Error: " + e.getMessage());
			System.exit(1);
		}
	}
}

