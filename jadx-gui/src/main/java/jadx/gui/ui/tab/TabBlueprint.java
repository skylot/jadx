package jadx.gui.ui.tab;

import jadx.gui.treemodel.JNode;

public class TabBlueprint {
	private final JNode node;
	private boolean pinned;
	private boolean bookmarked;

	public TabBlueprint(JNode node) {
		this.node = node;
	}

	public JNode getNode() {
		return node;
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

	public boolean isPinnable() {
		return node.isPinnable();
	}
}
