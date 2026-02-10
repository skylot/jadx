package jadx.gui.treemodel;

import javax.swing.Icon;

import jadx.core.utils.StringUtils;

public class JResSearchNode extends JNode {
	private static final long serialVersionUID = -2222084945157778639L;
	private final transient JResource resNode;
	private final transient String text;
	private final transient int pos;

	public JResSearchNode(JResource resNode, String text, int pos) {
		this.pos = pos;
		this.text = text;
		this.resNode = resNode;
	}

	public JResource getResNode() {
		return resNode;
	}

	public int getPos() {
		return pos;
	}

	@Override
	public String makeDescString() {
		return text;
	}

	@Override
	public JClass getJParent() {
		return resNode.getJParent();
	}

	@Override
	public String makeLongStringHtml() {
		return getName();
	}

	@Override
	public Icon getIcon() {
		return resNode.getIcon();
	}

	@Override
	public String getName() {
		return resNode.getName();
	}

	@Override
	public String makeString() {
		return resNode.makeString();
	}

	@Override
	public boolean hasDescString() {
		return !StringUtils.isEmpty(text);
	}
}
