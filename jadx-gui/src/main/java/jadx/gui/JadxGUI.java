package jadx.gui;

import jadx.cli.JadxArgs;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JadxGUI {
	private static final Logger LOG = LoggerFactory.getLogger(JadxGUI.class);

	public static void main(String[] args) {
		final JadxArgs jadxArgs = new JadxArgs(args, false);

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Throwable e) {
			LOG.error("Error: " + e.getMessage());
			System.exit(1);
		}

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				MainWindow mainWindow = new MainWindow(jadxArgs);
				mainWindow.setVisible(true);

				if (!jadxArgs.getInput().isEmpty()) {
					mainWindow.openFile(jadxArgs.getInput().get(0));
				}
			}
		});
	}
}

