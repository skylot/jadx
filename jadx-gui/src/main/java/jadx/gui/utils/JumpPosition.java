package jadx.gui.utils;

import jadx.gui.treemodel.JNode;

public class JumpPosition {
	private final JNode node;
	private final int line;
	// the position of the node in java code,
	// call codeArea.scrollToPos(pos) to set caret
	private int pos;
	// Precise means caret can be set right at the node in codeArea,
	// not just the start of the line.
	private boolean precise;

	public JumpPosition(JNode node, int line) {
		this(node, line, 0);
	}

	public JumpPosition(JNode node, int line, int pos) {
		this.node = node;
		this.line = line;
		this.pos = pos;
	}

	public boolean isPrecise() {
		return precise;
	}

	public JumpPosition setPrecise(int pos) {
		this.pos = pos;
		this.precise = true;
		return this;
	}

	public int getPos() {
		return pos;
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
		return line == position.line && node.equals(position.node) && pos == position.pos;
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
