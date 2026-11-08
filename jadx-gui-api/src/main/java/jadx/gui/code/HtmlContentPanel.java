package jadx.gui.code;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;

import jadx.api.gui.IMainWindow;
import jadx.api.gui.code.JadxContentPanel;
import jadx.api.gui.tree.ITreeNode;

public class HtmlContentPanel extends JadxContentPanel {
	private static final long serialVersionUID = 1099513724770405877L;

	private final IMainWindow mainWindow;
	private final JHtmlPane textArea;

	public HtmlContentPanel(IMainWindow mainWindow, ITreeNode node, String content) {
		super(node);
		this.mainWindow = mainWindow;

		setLayout(new BorderLayout());
		textArea = new JHtmlPane();
		textArea.setEditable(false);
		loadSettings();
		loadContent(content);
		add(new JScrollPane(textArea));
		mainWindow.registerZoomActions(textArea, this::loadSettings);
	}

	@Override
	public void loadSettings() {
		textArea.setFont(mainWindow.getSettings().getUiFont());
	}

	public void loadContent(String content) {
		textArea.setText(content);
		textArea.setCaretPosition(0); // otherwise the start view will be the last line
	}

	public JEditorPane getHtmlArea() {
		return textArea;
	}

	private static final class JHtmlPane extends JEditorPane {
		private static final long serialVersionUID = 1753360686756555655L;

		public JHtmlPane() {
			setContentType("text/html");
		}

		@Override
		public void paint(Graphics g) {
			Graphics2D g2d = (Graphics2D) g.create();
			try {
				g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				super.paint(g2d);
			} finally {
				g2d.dispose();
			}
		}
	}
}
