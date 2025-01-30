package jadx.gui.treemodel;

import javax.swing.Icon;

public class SearchResultNode extends JNode {

	private final String label;
	private final JNode realNode;

	public SearchResultNode(String str, JNode realNode, int start, int end) {
		this.label = str;
		this.realNode = realNode;
		this.start = start;
		this.end = end;
		this.hasHighlight = true;
	}

	@Override
	public JClass getJParent() {
		return null;
	}

	@Override
	public Icon getIcon() {
		return null;
	}

	@Override
	public String makeString() {
		return label;
	}

	@Override
	public String makeLongString() {
		return makeString();
	}

	public JNode getRealNode() {
		return realNode;
	}

	@Override
	public boolean disableHtml() {
		return false;
	}
}
