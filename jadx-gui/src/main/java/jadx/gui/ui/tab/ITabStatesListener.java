package jadx.gui.ui.tab;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.gui.ui.codearea.EditorViewState;
import jadx.gui.utils.JumpPosition;

/**
 * Tabbed pane events listener
 */
public interface ITabStatesListener {

	/**
	 * Tab added to tabbed pane without become active (selected)
	 */
	default void onTabOpen(TabBlueprint blueprint) {
	}

	/**
	 * Tab become active (selected)
	 */
	default void onTabSelect(TabBlueprint blueprint) {
	}

	/**
	 * Caret position changes.
	 *
	 * @param prevPos previous caret position; can be null if unknown; can be from another tab
	 * @param newPos  new caret position, node refer to jump target node
	 */
	default void onTabCodeJump(TabBlueprint blueprint, @Nullable JumpPosition prevPos, JumpPosition newPos) {
	}

	default void onTabSmaliJump(TabBlueprint blueprint, int pos, boolean debugMode) {
	}

	default void onTabClose(TabBlueprint blueprint) {
	}

	default void onTabPositionFirst(TabBlueprint blueprint) {
	}

	default void onTabPinChange(TabBlueprint blueprint) {
	}

	default void onTabBookmarkChange(TabBlueprint blueprint) {
	}

	default void onTabVisibilityChange(TabBlueprint blueprint) {
	}

	default void onTabRestore(TabBlueprint blueprint, EditorViewState viewState) {
	}

	default void onTabsRestoreDone() {
	}

	default void onTabsReorder(List<TabBlueprint> blueprints) {
	}

	default void onTabSave(TabBlueprint blueprint, EditorViewState viewState) {
	}

	default void onTabPreviewChange(TabBlueprint blueprint) {
	}
}
