package jadx.gui.ui;

import jadx.gui.treemodel.JNode;
import jadx.gui.ui.codearea.CodeArea;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import javax.swing.*;
import javax.swing.plaf.PanelUI;
import java.awt.*;

public final class HtmlPanel extends ContentPanel {
	private static final long serialVersionUID = -6251262855835426245L;

	private final JEditorPane textArea;

	public HtmlPanel(TabbedPane panel, JNode jnode) {
		super(panel, jnode);
		setLayout(new BorderLayout());
		textArea = new JEditorPane() {

			public void paint(Graphics g) {
				Graphics2D g2d = (Graphics2D) g.create();
				try {
					g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
					super.paint(g2d);
				} finally {
					g2d.dispose();
				}
			}

		};
		textArea.setContentType("text/html");
		textArea.setText(jnode.getContent());
		JScrollPane sp = new JScrollPane(textArea);
		add(sp);
	}

	@Override
	public void loadSettings() {

	}

}
