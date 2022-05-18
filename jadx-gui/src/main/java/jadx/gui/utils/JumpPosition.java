package jadx.gui.utils;

import jadx.core.utils.Utils;
import jadx.gui.treemodel.JNode;

public class JumpPosition {
	private final JNode node;
	private int pos;

	public JumpPosition(JNode node) {
		this(node, node.getPos());
	}

	public JumpPosition(JNode node, int pos) {
		this.node = Utils.getOrElse(node.getRootClass(), node);
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

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof JumpPosition)) {
			return false;
		}
		JumpPosition jump = (JumpPosition) obj;
		return pos == jump.pos && node.equals(jump.node);
	}

	@Override
	public int hashCode() {
		return 31 * node.hashCode() + pos;
	}

	@Override
	public String toString() {
		return "Jump: " + node + " : " + pos;
	}
}
