package jadx.gui.utils;

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.awt.Desktop.Action;

public class Link extends JLabel {
	private static final long serialVersionUID = 3655322136444908178L;

	private static final Logger LOG = LoggerFactory.getLogger(Link.class);

	private final String url;

	public Link(String text, String url) {
		super(text);
		this.url = url;
		setText(text);
		setToolTipText("Open " + url + " in your browser");
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				browse();
			}
		});
	}

	private void browse() {
		if (Desktop.isDesktopSupported()) {
			Desktop desktop = Desktop.getDesktop();
			if (desktop.isSupported(Action.BROWSE)) {
				try {
					desktop.browse(new java.net.URI(url));
					return;
				} catch (Exception e) {
					LOG.debug("Open url error", e);
				}
			}
		}
		try {
			if (SystemInfo.IS_WINDOWS) {
				new ProcessBuilder()
						.command(new String[] { "rundll32", "url.dll,FileProtocolHandler", url })
						.start();
				return;
			}
			if (SystemInfo.IS_MAC) {
				new ProcessBuilder()
						.command(new String[] { "open", url })
						.start();
				return;
			}
			Map<String, String> env = System.getenv();
			if (env.get("BROWSER") != null) {
				new ProcessBuilder()
						.command(new String[] { env.get("BROWSER"), url })
						.start();
				return;
			}
		} catch (Exception e) {
			LOG.debug("Open url error", e);
		}
		showUrlDialog();
	}

	private void showUrlDialog() {
		JTextArea urlArea = new JTextArea("Can't open browser. Please browse to:\n" + url);
		JOptionPane.showMessageDialog(null, urlArea);
	}
}
