package jadx.gui.utils;

import jadx.gui.treemodel.JNode;

public class JumpPosition {
	private final JNode node;
	private final int line;

	public JumpPosition(JNode node, int line) {
		this.node = node;
		this.line = line;
	}

	public JNode getNode() {
		return node;
	}

	public int getLine() {
		return line;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof JumpPosition)) {
			return false;
		}
		JumpPosition position = (JumpPosition) obj;
		return line == position.line && node.equals(position.node);
	}

	@Override
	public int hashCode() {
		return 31 * node.hashCode() + line;
	}

	@Override
	public String toString() {
		return "Position: " + node + " : " + line;
	}
}
