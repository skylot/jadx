package jadx.gui.ui.codearea;

import java.awt.Point;

import jadx.gui.treemodel.JNode;

public class EditorViewState {
	public static final Point ZERO = new Point(0, 0);

	private final JNode node;
	private int caretPos;
	private Point viewPoint;
	private String subPath;

	private boolean active;

	private boolean pinned;
	private boolean bookmarked;
	private boolean hidden;
	private boolean previewTab;

	public EditorViewState(JNode node) {
		this(node, "", 0, EditorViewState.ZERO);
	}

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

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isPinned() {
		return pinned;
	}

	public void setPinned(boolean pinned) {
		this.pinned = pinned;
	}

	public boolean isBookmarked() {
		return bookmarked;
	}

	public void setBookmarked(boolean bookmarked) {
		this.bookmarked = bookmarked;
	}

	public boolean isHidden() {
		return hidden;
	}

	public boolean isPreviewTab() {
		return previewTab;
	}

	public void setPreviewTab(boolean previewTab) {
		this.previewTab = previewTab;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
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
