package jadx.gui.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.awt.Desktop.Action;

public class Link extends JLabel implements MouseListener {
	private static final long serialVersionUID = 3655322136444908178L;

	private static final Logger LOG = LoggerFactory.getLogger(Link.class);

	private String url;

	public Link(String text, String url) {
		super(text);
		this.url = url;
		this.setToolTipText("Open " + url + " in your browser");
		this.addMouseListener(this);
		this.setForeground(Color.BLUE);
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		browse();
	}

	@Override
	public void mouseEntered(MouseEvent arg0) {
		setCursor(new Cursor(Cursor.HAND_CURSOR));
	}

	@Override
	public void mouseExited(MouseEvent arg0) {
		setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
	}

	@Override
	public void mousePressed(MouseEvent arg0) {
		// ignore
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
		// ignore
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
			String os = System.getProperty("os.name").toLowerCase();
			if (os.contains("win")) {
				new ProcessBuilder()
					.command(new String[] {"rundll32", "url.dll,FileProtocolHandler", url})
					.start();
				return;
			}
			if (os.contains("mac")) {
				new ProcessBuilder()
					.command(new String[] {"open", url})
					.start();
				return;
			}
			Map<String, String> env = System.getenv();
			if (env.get("BROWSER") != null) {
				new ProcessBuilder()
					.command(new String[] {env.get("BROWSER"), url})
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
