package jadx.gui.ui.tab;

import java.util.List;

import jadx.gui.ui.codearea.EditorViewState;
import jadx.gui.utils.JumpPosition;

public interface ITabStatesListener {

	default void onTabOpen(TabBlueprint blueprint) {
	}

	default void onTabSelect(TabBlueprint blueprint) {
	}

	default void onTabCodeJump(TabBlueprint blueprint, JumpPosition position) {
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

}
