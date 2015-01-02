package jadx.gui.utils;

import jadx.api.CodePosition;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;

public class Position {
	private final JNode node;
	private final int line;

	public Position(CodePosition pos) {
		this.node = new JClass(pos.getJavaClass());
		this.line = pos.getLine();
	}

	public Position(JNode node, int line) {
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
		if (!(obj instanceof Position)) {
			return false;
		}
		Position position = (Position) obj;
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
