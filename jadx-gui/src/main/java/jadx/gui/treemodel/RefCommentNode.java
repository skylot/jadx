package jadx.gui.treemodel;

import javax.swing.Icon;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import jadx.api.JavaNode;

public class RefCommentNode extends JNode {
	private static final long serialVersionUID = 3887992236082515752L;

	protected final JNode node;
	protected final String comment;

	public RefCommentNode(JNode node, String comment) {
		this.node = node;
		this.comment = comment;
	}

	public JNode getNode() {
		return node;
	}

	@Override
	public JClass getRootClass() {
		return node.getRootClass();
	}

	@Override
	public JavaNode getJavaNode() {
		return node.getJavaNode();
	}

	@Override
	public JClass getJParent() {
		return node.getJParent();
	}

	@Override
	public Icon getIcon() {
		return node.getIcon();
	}

	@Override
	public String getSyntaxName() {
		return SyntaxConstants.SYNTAX_STYLE_NONE; // comment is always plain text
	}

	@Override
	public String makeString() {
		return node.makeString();
	}

	@Override
	public String makeLongString() {
		return node.makeLongString();
	}

	@Override
	public String makeStringHtml() {
		return node.makeStringHtml();
	}

	@Override
	public String makeLongStringHtml() {
		return node.makeLongStringHtml();
	}

	@Override
	public boolean disableHtml() {
		return node.disableHtml();
	}

	@Override
	public int getPos() {
		return node.getPos();
	}

	@Override
	public String getTooltip() {
		return node.getTooltip();
	}

	@Override
	public String makeDescString() {
		return comment;
	}

	@Override
	public boolean hasDescString() {
		return true;
	}
}
