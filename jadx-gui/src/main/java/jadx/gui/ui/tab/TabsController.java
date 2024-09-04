package jadx.gui.ui.tab;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JavaClass;
import jadx.api.metadata.ICodeAnnotation;
import jadx.api.metadata.ICodeNodeRef;
import jadx.api.metadata.annotations.NodeDeclareRef;
import jadx.gui.jobs.SimpleTask;
import jadx.gui.jobs.TaskStatus;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.EditorViewState;
import jadx.gui.utils.JumpPosition;

public class TabsController {
	private static final Logger LOG = LoggerFactory.getLogger(TabsController.class);

	private final MainWindow mainWindow;
	private final Map<JNode, TabBlueprint> tabsMap = new HashMap<>();
	private final List<ITabStatesListener> listeners = new ArrayList<>();

	private boolean forceClose;

	private @Nullable TabBlueprint selectedTab;

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
		return openTab(node, false);
	}

	public TabBlueprint openTab(JNode node, boolean hidden) {
		TabBlueprint blueprint = getTabByNode(node);
		if (blueprint == null) {
			TabBlueprint newBlueprint = new TabBlueprint(node);
			tabsMap.put(node, newBlueprint);
			newBlueprint.setHidden(hidden);
			listeners.forEach(l -> l.onTabOpen(newBlueprint));
			if (hidden) {
				listeners.forEach(l -> l.onTabVisibilityChange(newBlueprint));
			}
			blueprint = newBlueprint;
		}
		setTabHiddenInternal(blueprint, hidden);
		return blueprint;
	}

	public void selectTab(JNode node) {
		TabBlueprint blueprint = openTab(node);
		selectedTab = blueprint;
		listeners.forEach(l -> l.onTabSelect(blueprint));
	}

	/**
	 * Jump to node definition
	 */
	public void codeJump(JNode node) {
		JClass parentCls = node.getJParent();
		if (parentCls != null) {
			JavaClass cls = node.getJParent().getCls();
			JavaClass origTopCls = cls.getOriginalTopParentClass();
			JavaClass codeParent = cls.getTopParentClass();
			if (!Objects.equals(codeParent, origTopCls)) {
				JClass jumpCls = mainWindow.getCacheObject().getNodeCache().makeFrom(codeParent);
				loadCodeWithUIAction(jumpCls, () -> jumpToInnerClass(node, codeParent, jumpCls));
				return;
			}
		}

		// Not an inline node, jump normally
		if (node.getPos() != 0 || node.getRootClass() == null) {
			codeJump(new JumpPosition(node));
			return;
		}
		// node need loading
		loadCodeWithUIAction(node.getRootClass(), () -> codeJump(new JumpPosition(node)));
	}

	private void loadCodeWithUIAction(JClass cls, Runnable action) {
		SimpleTask loadTask = cls.getLoadTask();
		mainWindow.getBackgroundExecutor().execute(
				new SimpleTask(loadTask.getTitle(),
						loadTask.getJobs(),
						status -> {
							Consumer<TaskStatus> onFinish = loadTask.getOnFinish();
							if (onFinish != null) {
								onFinish.accept(status);
							}
							action.run();
						}));
	}

	/**
	 * Search and jump to original node in jumpCls
	 */
	private void jumpToInnerClass(JNode node, JavaClass codeParent, JClass jumpCls) {
		codeParent.getCodeInfo().getCodeMetadata().searchDown(0, (pos, ann) -> {
			if (ann.getAnnType() == ICodeAnnotation.AnnType.DECLARATION) {
				ICodeNodeRef declNode = ((NodeDeclareRef) ann).getNode();
				if (declNode.equals(node.getJavaNode().getCodeNodeRef())) {
					codeJump(new JumpPosition(jumpCls, pos));
					return true;
				}
			}
			return null;
		});
	}

	/**
	 * Prefer {@link TabsController#codeJump(JNode)} method
	 */
	public void codeJump(JumpPosition pos) {
		if (selectedTab == null) {
			LOG.warn("Cannot codeJump because selectedTab is null");
			return;
		}
		listeners.forEach(l -> l.onTabCodeJump(selectedTab, pos));
	}

	public void smaliJump(JClass cls, int pos, boolean debugMode) {
		TabBlueprint blueprint = openTab(cls);
		listeners.forEach(l -> l.onTabSmaliJump(blueprint, pos, debugMode));
	}

	public void closeTab(JNode node) {
		closeTab(node, false);
	}

	public void closeTab(JNode node, boolean considerPins) {
		TabBlueprint blueprint = getTabByNode(node);

		if (blueprint == null) {
			return;
		}

		if (forceClose) {
			closeTabForce(blueprint);
			return;
		}

		if (!considerPins || !blueprint.isPinned()) {
			if (!blueprint.isReferenced()) {
				closeTabForce(blueprint);
			} else {
				closeTabSoft(blueprint);
			}
		}
	}

	/*
	 * Removes Tab from everywhere
	 */
	private void closeTabForce(TabBlueprint blueprint) {
		listeners.forEach(l -> l.onTabClose(blueprint));
		tabsMap.remove(blueprint.getNode());
	}

	/*
	 * Hides Tab from TabbedPane
	 */
	private void closeTabSoft(TabBlueprint blueprint) {
		setTabHidden(blueprint.getNode(), true);
	}

	public void setTabPositionFirst(JNode node) {
		TabBlueprint blueprint = openTab(node);
		listeners.forEach(l -> l.onTabPositionFirst(blueprint));
	}

	public void setTabPinned(JNode node, boolean pinned) {
		TabBlueprint blueprint = openTab(node);
		setTabPinnedInternal(blueprint, pinned);
	}

	public void setTabPinnedInternal(TabBlueprint blueprint, boolean pinned) {
		if (blueprint.isPinned() != pinned) {
			blueprint.setPinned(pinned);
			listeners.forEach(l -> l.onTabPinChange(blueprint));
		}
	}

	public void setTabBookmarked(JNode node, boolean bookmarked) {
		TabBlueprint blueprint = openTab(node);
		setTabBookmarkedInternal(blueprint, bookmarked);
	}

	private void setTabBookmarkedInternal(TabBlueprint blueprint, boolean bookmarked) {
		if (blueprint.isBookmarked() != bookmarked) {
			blueprint.setBookmarked(bookmarked);
			listeners.forEach(l -> l.onTabBookmarkChange(blueprint));
			removeTabIfNotReferenced(blueprint);
		}
	}

	public void setTabHidden(JNode node, boolean hidden) {
		TabBlueprint blueprint = getTabByNode(node);
		setTabHiddenInternal(blueprint, hidden);
	}

	private void setTabHiddenInternal(TabBlueprint blueprint, boolean hidden) {
		if (blueprint != null && blueprint.isHidden() != hidden) {
			blueprint.setHidden(hidden);
			listeners.forEach(l -> l.onTabVisibilityChange(blueprint));
		}
	}

	private void removeTabIfNotReferenced(TabBlueprint blueprint) {
		if (blueprint.isHidden() && !blueprint.isReferenced()) {
			tabsMap.remove(blueprint.getNode());
		}
	}

	public void closeAllTabs() {
		closeAllTabs(false);
	}

	public void forceCloseAllTabs() {
		forceClose = true;
		closeAllTabs();
		forceClose = false;
		selectedTab = null;
	}

	public boolean isForceClose() {
		return forceClose;
	}

	public void closeAllTabs(boolean considerPins) {
		List.copyOf(tabsMap.values()).forEach(t -> closeTab(t.getNode(), considerPins));
	}

	public void unpinAllTabs() {
		tabsMap.values().forEach(t -> setTabPinned(t.getNode(), false));
	}

	public void unbookmarkAllTabs() {
		tabsMap.values().forEach(t -> setTabBookmarked(t.getNode(), false));
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

	public List<TabBlueprint> getBookmarkedTabs() {
		return tabsMap.values().stream()
				.filter(TabBlueprint::isBookmarked)
				.collect(Collectors.toUnmodifiableList());
	}

	public void restoreEditorViewState(EditorViewState viewState) {
		JNode node = viewState.getNode();
		TabBlueprint blueprint = openTab(node, viewState.isHidden());
		setTabPinnedInternal(blueprint, viewState.isPinned());
		setTabBookmarkedInternal(blueprint, viewState.isBookmarked());
		listeners.forEach(l -> l.onTabRestore(blueprint, viewState));
		if (viewState.isActive()) {
			selectTab(node);
		}
	}

	public void notifyRestoreEditorViewStateDone() {
		if (selectedTab == null && !tabsMap.isEmpty()) {
			JNode node = tabsMap.values().iterator().next().getNode();
			LOG.warn("No active tab found, select {}", node); // TODO: find the reason of this issue
			selectTab(node);
		}
		listeners.forEach(ITabStatesListener::onTabsRestoreDone);
	}

	public List<EditorViewState> getEditorViewStates() {
		List<TabBlueprint> reorderedTabs = new ArrayList<>(tabsMap.values());
		listeners.forEach(l -> l.onTabsReorder(reorderedTabs));
		List<EditorViewState> states = new ArrayList<>();
		for (TabBlueprint blueprint : reorderedTabs) {
			states.add(getEditorViewState(blueprint));
		}
		return states;
	}

	public EditorViewState getEditorViewState(TabBlueprint blueprint) {
		EditorViewState viewState = new EditorViewState(blueprint.getNode());
		listeners.forEach(l -> l.onTabSave(blueprint, viewState));
		viewState.setActive(blueprint == selectedTab);
		viewState.setPinned(blueprint.isPinned());
		viewState.setBookmarked(blueprint.isBookmarked());
		viewState.setHidden(blueprint.isHidden());
		return viewState;
	}
}
