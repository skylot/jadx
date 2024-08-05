package jadx.gui.ui.tab;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.EditorViewState;

public class TabsController {
	private final transient MainWindow mainWindow;
	private final Map<JNode, TabBlueprint> tabsMap = new HashMap<>();
	private final ArrayList<ITabStatesListener> listeners = new ArrayList<>();

	private TabBlueprint selectedTab = null;

	public TabsController(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
	}

	public MainWindow getMainWindow() {
		return mainWindow;
	}

	public void addListener(ITabStatesListener listener) {
		listeners.add(listener);
	}

	public void removeListener(ITabStatesListener listener) {
		listeners.remove(listener);
	}

	public @Nullable TabBlueprint getTabByNode(JNode node) {
		return tabsMap.get(node);
	}

	public TabBlueprint openTab(JNode node) {
		TabBlueprint blueprint = getTabByNode(node);
		if (blueprint == null) {
			TabBlueprint newBlueprint = new TabBlueprint(node);
			tabsMap.put(node, newBlueprint);
			listeners.forEach(l -> l.onTabOpen(newBlueprint));
			blueprint = newBlueprint;
		}
		return blueprint;
	}

	public void selectTab(JNode node) {
		TabBlueprint blueprint = openTab(node);
		selectedTab = blueprint;

		listeners.forEach(l -> l.onTabSelect(blueprint));
	}

	public void closeTab(JNode node) {
		closeTab(node, false);
	}

	public void closeTab(JNode node, boolean considerPins) {
		TabBlueprint blueprint = getTabByNode(node);

		if (blueprint != null && (!considerPins || !blueprint.isPinned())) {
			listeners.forEach(l -> l.onTabClose(blueprint));
		}

		tabsMap.remove(node);
	}

	public void setTabPositionFirst(JNode node) {
		TabBlueprint blueprint = openTab(node);
		listeners.forEach(l -> l.onTabPositionFirst(blueprint));
	}

	public void setTabPinned(JNode node, boolean pinned) {
		TabBlueprint blueprint = openTab(node);
		if (blueprint.isPinned() != pinned) {
			blueprint.setPinned(pinned);
			listeners.forEach(l -> l.onTabPinChange(blueprint));
		}
	}

	public void setTabBookmarked(JNode node, boolean bookmarked) {
		TabBlueprint blueprint = openTab(node);
		if (blueprint.isBookmarked() != bookmarked) {
			blueprint.setBookmarked(bookmarked);
			listeners.forEach(l -> l.onTabBookmarkChange(blueprint));
		}
	}

	public void closeAllTabs() {
		closeAllTabs(false);
	}

	public void closeAllTabs(boolean considerPins) {
		List.copyOf(tabsMap.values()).forEach(t -> closeTab(t.getNode(), considerPins));
	}

	public void unpinAllTabs() {
		tabsMap.values().forEach(t -> setTabPinned(t.getNode(), false));
	}

	public TabBlueprint getSelectedTab() {
		return selectedTab;
	}

	public List<TabBlueprint> getTabs() {
		return List.copyOf(tabsMap.values());
	}

	public List<TabBlueprint> getOpenTabs() {
		return List.copyOf(tabsMap.values());
	}

	public List<TabBlueprint> getPinnedTabs() {
		return tabsMap.values().stream()
				.filter(TabBlueprint::isPinned)
				.collect(Collectors.toUnmodifiableList());
	}

	public void restoreEditorViewState(EditorViewState viewState) {
		JNode node = viewState.getNode();
		TabBlueprint blueprint = openTab(node);
		setTabPinned(node, viewState.isPinned());
		if (viewState.isActive()) {
			selectTab(node);
		}
		listeners.forEach(l -> l.onTabRestore(blueprint, viewState));
	}

	public List<EditorViewState> getEditorViewStates() {
		List<EditorViewState> states = new ArrayList<>();
		for (TabBlueprint blueprint : getTabs()) {
			states.add(getEditorViewState(blueprint));
		}
		return states;
	}

	public EditorViewState getEditorViewState(TabBlueprint blueprint) {
		EditorViewState viewState = new EditorViewState(blueprint.getNode());
		listeners.forEach(l -> l.onTabSave(blueprint, viewState));
		viewState.setActive(blueprint == selectedTab);
		viewState.setPinned(blueprint.isPinned());
		return viewState;
	}
}
