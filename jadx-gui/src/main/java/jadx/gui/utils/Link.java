package jadx.gui.utils;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import static java.awt.Desktop.Action;

public class Link extends JLabel implements MouseListener {
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
	}

	@Override
	public void mouseReleased(MouseEvent arg0) {
	}

	private void browse() {
		if (Desktop.isDesktopSupported()) {
			Desktop desktop = Desktop.getDesktop();
			if (desktop.isSupported(Action.BROWSE)) {
				try {
					desktop.browse(new java.net.URI(url));
					return;
				} catch (IOException e) {
					e.printStackTrace();
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
			}
		}
		try {
			String os = System.getProperty("os.name").toLowerCase();
			if (os.contains("win")) {
				Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + url);
				return;
			}
			if (os.contains("mac")) {
				Runtime.getRuntime().exec("open " + url);
				return;
			}
			Map<String, String> env = System.getenv();
			if (env.get("BROWSER") != null) {
				Runtime.getRuntime().exec(env.get("BROWSER") + " " + url);
				return;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		showUrlDialog();
	}

	private void showUrlDialog() {
		JTextArea urlArea = new JTextArea("Can't open browser. Please browse to:\n"+url);
		JOptionPane.showMessageDialog(null, urlArea);
	}
}
