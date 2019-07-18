package jadx.gui.ui.codearea;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.TextNode;
import jadx.gui.ui.ContentPanel;

public final class SmaliArea extends AbstractCodeArea {
	private static final long serialVersionUID = 1334485631870306494L;

	private final JNode textNode;

	SmaliArea(ContentPanel contentPanel) {
		super(contentPanel);
		this.textNode = new TextNode(node.getName());
		setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
	}

	@Override
	public void load() {
		if (getText().isEmpty()) {
			setText(node.getSmali());
			setCaretPosition(0);
		}
	}

	@Override
	public JNode getNode() {
		// this area contains only smali without other node attributes
		return textNode;
	}
}
