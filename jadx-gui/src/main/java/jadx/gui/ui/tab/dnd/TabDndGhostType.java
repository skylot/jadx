package jadx.gui.ui.tab.dnd;

public enum TabDndGhostType {
	/**
	 * Bitmap is rendered from tabs component and dragged along with cursor.
	 * May be impactful on performance.
	 */
	IMAGE,

	/**
	 * Colored rect of tabs size is dragged along with cursor.
	 */
	COLORFUL_RECT,

	/**
	 * Only insert mark is rendered.
	 */
	NONE,
}
