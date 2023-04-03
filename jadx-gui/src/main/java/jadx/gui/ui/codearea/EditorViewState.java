package jadx.gui.ui.codearea;

import java.awt.Point;

import jadx.gui.treemodel.JNode;

public class EditorViewState {
	public static final Point ZERO = new Point(0, 0);

	private final JNode node;
	private final int caretPos;
	private final Point viewPoint;
	private final String subPath;
	private boolean active;

	public EditorViewState(JNode node, String subPath, int caretPos, Point viewPoint) {
		this.node = node;
		this.subPath = subPath;
		this.caretPos = caretPos;
		this.viewPoint = viewPoint;
	}

	public JNode getNode() {
		return node;
	}

	public int getCaretPos() {
		return caretPos;
	}

	public Point getViewPoint() {
		return viewPoint;
	}

	public String getSubPath() {
		return subPath;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	@Override
	public String toString() {
		return "EditorViewState{node=" + node
				+ ", caretPos=" + caretPos
				+ ", viewPoint=" + viewPoint
				+ ", subPath='" + subPath + '\''
				+ ", active=" + active
				+ '}';
	}
}
