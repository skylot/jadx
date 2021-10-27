package jadx.gui.ui.codearea;

import java.awt.Point;

import jadx.gui.treemodel.JNode;

public class EditorViewState {
	private JNode node;
	private int caretPos;
	private Point viewPoint;
	private String subPath;

	public EditorViewState(JNode node, String subPath, int caretPos, Point viewPoint) {
		this.node = node;
		this.subPath = subPath;
		this.caretPos = caretPos;
		this.viewPoint = viewPoint;
	}

	public JNode getNode() {
		return node;
	}

	public void setNode(JNode node) {
		this.node = node;
	}

	public int getCaretPos() {
		return caretPos;
	}

	public void setCaretPos(int caretPos) {
		this.caretPos = caretPos;
	}

	public Point getViewPoint() {
		return viewPoint;
	}

	public void setViewPoint(Point viewPoint) {
		this.viewPoint = viewPoint;
	}

	public String getSubPath() {
		return subPath;
	}

	public void setSubPath(String subPath) {
		this.subPath = subPath;
	}

	@Override
	public String toString() {
		return "EditorViewState{node=" + node
				+ ", caretPos=" + caretPos
				+ ", viewPoint=" + viewPoint
				+ ", subPath='" + subPath + '\''
				+ '}';
	}
}
