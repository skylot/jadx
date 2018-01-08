package jadx.gui.treemodel;

import javax.swing.*;

public class TextNode extends JNode {

	private static final long serialVersionUID = 2342749142368352232L;

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
	public Icon getIcon() {
		return null;
	}

	@Override
	public String makeString() {
		return label;
	}
}
