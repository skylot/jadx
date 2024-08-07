package jadx.gui.ui.tab;

import java.util.List;

import jadx.gui.ui.codearea.EditorViewState;
import jadx.gui.utils.JumpPosition;

public interface ITabStatesListener {
	void onTabOpen(TabBlueprint blueprint);

	void onTabSelect(TabBlueprint blueprint);

	void onTabCodeJump(TabBlueprint blueprint, JumpPosition position);

	void onTabSmaliJump(TabBlueprint blueprint, int pos, boolean debugMode);

	void onTabClose(TabBlueprint blueprint);

	void onTabPositionFirst(TabBlueprint blueprint);

	void onTabPinChange(TabBlueprint blueprint);

	void onTabBookmarkChange(TabBlueprint blueprint);

	void onTabVisibilityChange(TabBlueprint blueprint);

	void onTabRestore(TabBlueprint blueprint, EditorViewState viewState);

	void onTabsRestoreDone();

	void onTabsReorder(List<TabBlueprint> blueprints);

	void onTabSave(TabBlueprint blueprint, EditorViewState viewState);
}
