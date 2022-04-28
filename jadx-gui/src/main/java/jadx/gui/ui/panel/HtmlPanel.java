package jadx.gui.ui.panel;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;

import jadx.gui.settings.JadxSettings;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.TabbedPane;
import jadx.gui.utils.ui.ZoomActions;

public final class HtmlPanel extends ContentPanel {
	private static final long serialVersionUID = -6251262855835426245L;

	private final JHtmlPane textArea;

	public HtmlPanel(TabbedPane panel, JNode jnode) {
		super(panel, jnode);
		setLayout(new BorderLayout());
		textArea = new JHtmlPane();
		loadSettings();
		textArea.setText(jnode.getCodeInfo().getCodeStr());
		textArea.setCaretPosition(0); // otherwise the start view will be the last line
		textArea.setEditable(false);
		JScrollPane sp = new JScrollPane(textArea);
		add(sp);

		ZoomActions.register(textArea, panel.getMainWindow().getSettings(), this::loadSettings);
	}

	@Override
	public void loadSettings() {
		JadxSettings settings = getTabbedPane().getMainWindow().getSettings();
		textArea.setFont(settings.getFont());
	}

	public JEditorPane getHtmlArea() {
		return textArea;
	}

	private static final class JHtmlPane extends JEditorPane {
		private static final long serialVersionUID = 6886040384052136157L;

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
