package jadx.gui.ui.tab;

import java.util.ArrayList;

import jadx.gui.ui.codearea.EditorViewState;

public interface ITabStatesListener {
	void onTabOpen(TabBlueprint blueprint);

	void onTabSelect(TabBlueprint blueprint);

	void onTabClose(TabBlueprint blueprint);

	void onTabPositionFirst(TabBlueprint blueprint);

	void onTabPinChange(TabBlueprint blueprint);

	void onTabBookmarkChange(TabBlueprint blueprint);

	void onTabVisibilityChange(TabBlueprint blueprint);

	void onTabRestore(TabBlueprint blueprint, EditorViewState viewState);

	void onTabsRestoreDone();

	void onTabsReorder(ArrayList<TabBlueprint> blueprints);

	void onTabSave(TabBlueprint blueprint, EditorViewState viewState);
}
