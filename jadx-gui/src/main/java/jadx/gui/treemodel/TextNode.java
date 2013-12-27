package jadx.gui.treemodel;

import javax.swing.Icon;

public class TextNode extends JNode {
	private final String label;

	public TextNode(String str) {
		this.label = str;
	}

	@Override
	public JClass getJParent() {
		return null;
	}

	@Override
	public int getLine() {
		return 0;
	}

	@Override
	public void updateChilds() {
	}

	@Override
	public Icon getIcon() {
		return null;
	}

	@Override
	public String toString() {
		return label;
	}
}
