package jadx.gui.utils;

import java.util.Objects;

import jadx.api.CodePosition;
import jadx.api.JavaNode;
import jadx.gui.treemodel.JNode;

public class JumpPosition {
	private final JNode node;
	private final int line;
	private int pos;

	public JumpPosition(JNode jumpNode) {
		this(Objects.requireNonNull(jumpNode.getRootClass()), jumpNode.getLine(), jumpNode.getPos());
	}

	public JumpPosition(JNode jumpNode, CodePosition codePos) {
		this(Objects.requireNonNull(jumpNode.getRootClass()), codePos.getLine(), codePos.getPos());
	}

	public JumpPosition(JNode node, int line, int pos) {
		this.node = node;
		this.line = line;
		this.pos = pos;
	}

	public int getPos() {
		return pos;
	}

	public void setPos(int pos) {
		this.pos = pos;
	}

	public JNode getNode() {
		return node;
	}

	public int getLine() {
		return line;
	}

	public static int getDefPos(JNode node) {
		JavaNode javaNode = node.getJavaNode();
		if (javaNode == null) {
			return -1;
		}
		return javaNode.getDefPos();
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
