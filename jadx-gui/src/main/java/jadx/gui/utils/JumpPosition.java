package jadx.gui.utils;

import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.treemodel.*;

public class JumpPosition {
	private final JNode node;
	private final int line;
	private int pos;
	private boolean precise;

	public JumpPosition(JNode node, int line) {
		this(node, line, -1);
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

	public static int getDefPos(JNode node) {
		if (node instanceof JClass) {
			return ((JClass) node).getCls().getClassNode().getDefPosition();
		}
		if (node instanceof JMethod) {
			return ((JMethod) node).getJavaMethod().getMethodNode().getDefPosition();
		}
		if (node instanceof JField) {
			return ((JField) node).getJavaField().getFieldNode().getDefPosition();
		}
		throw new JadxRuntimeException("Unexpected node " + node);
	}

	public static int getDefPos(JavaNode node) {
		if (node instanceof JavaClass) {
			return ((JavaClass) node).getClassNode().getDefPosition();
		}
		if (node instanceof JavaMethod) {
			return ((JavaMethod) node).getMethodNode().getDefPosition();
		}
		if (node instanceof JavaField) {
			return ((JavaField) node).getFieldNode().getDefPosition();
		}
		throw new JadxRuntimeException("Unexpected node " + node);
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
