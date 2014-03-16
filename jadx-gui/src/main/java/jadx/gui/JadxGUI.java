package jadx.gui;

import jadx.cli.JadxCLIArgs;
import jadx.gui.ui.MainWindow;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

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
					MainWindow window = new MainWindow(wrapper);
					window.pack();
					window.setLocationAndPosition();
					window.setVisible(true);
					window.setLocationRelativeTo(null);
					window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

					if (jadxArgs.getInput().isEmpty()) {
						window.openFile();
					} else {
						window.openFile(jadxArgs.getInput().get(0));
					}
				}
			});
		} catch (Throwable e) {
			LOG.error("Error: " + e.getMessage());
			System.exit(1);
		}
	}
}

