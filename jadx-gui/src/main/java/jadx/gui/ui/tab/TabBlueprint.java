package jadx.gui.ui.tab;

import java.util.Objects;

import jadx.gui.treemodel.JNode;

public class TabBlueprint {
	private final JNode node;
	private boolean created;
	private boolean pinned;
	private boolean bookmarked;
	private boolean hidden;
	private boolean previewTab;

	public TabBlueprint(JNode node) {
		this.node = Objects.requireNonNull(node);
	}

	public JNode getNode() {
		return node;
	}

	public boolean isCreated() {
		return created;
	}

	public void setCreated(boolean created) {
		this.created = created;
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

	public boolean supportsQuickTabs() {
		return node.supportsQuickTabs();
	}

	public boolean isReferenced() {
		return isBookmarked();
	}

	public boolean isHidden() {
		return hidden;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	public boolean isPreviewTab() {
		return previewTab;
	}

	public void setPreviewTab(boolean previewTab) {
		this.previewTab = previewTab;
	}

	@Override
	public final boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof TabBlueprint)) {
			return false;
		}
		return node.equals(((TabBlueprint) o).node);
	}

	@Override
	public int hashCode() {
		return node.hashCode();
	}

	@Override
	public String toString() {
		return "TabBlueprint{" + "node="
				+ node + ", pinned="
				+ pinned + ", bookmarked="
				+ bookmarked + ", hidden="
				+ hidden + ", previewTab="
				+ previewTab + '}';
	}
}
