package jadx.gui.utils;

import jadx.api.CodePosition;
import jadx.gui.treemodel.JClass;

public class Position {
	private final JClass cls;
	private final int line;

	public Position(CodePosition pos) {
		this.cls = new JClass(pos.getJavaClass());
		this.line = pos.getLine();
	}

	public Position(JClass cls, int line) {
		this.cls = cls;
		this.line = line;
	}

	public JClass getCls() {
		return cls;
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
		return line == position.line && cls.equals(position.cls);
	}

	@Override
	public int hashCode() {
		return 31 * cls.hashCode() + line;
	}

	@Override
	public String toString() {
		return "Position: " + cls + " : " + line;
	}
}
