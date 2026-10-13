package jadx.gui.ui.tab;

import java.awt.Component;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.andrewauclair.moderndocking.DockingRegion;
import io.github.andrewauclair.moderndocking.app.Docking;
import io.github.andrewauclair.moderndocking.event.DockingEvent;
import io.github.andrewauclair.moderndocking.event.DockingListener;

import jadx.core.utils.ListUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.jobs.SilentTask;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JEditableNode;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.action.ActionModel;
import jadx.gui.ui.action.JadxGuiAction;
import jadx.gui.ui.codearea.AbstractCodeArea;
import jadx.gui.ui.codearea.AbstractCodeContentPanel;
import jadx.gui.ui.codearea.ClassCodeContentPanel;
import jadx.gui.ui.codearea.EditorViewState;
import jadx.gui.ui.codearea.SmaliArea;
import jadx.gui.ui.dock.DockPanel;
import jadx.gui.ui.dock.EditorDock;
import jadx.gui.ui.dock.SplitSync;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.ui.panel.FontPanel;
import jadx.gui.ui.panel.HtmlPanel;
import jadx.gui.ui.panel.IViewStateSupport;
import jadx.gui.ui.panel.ImagePanel;
import jadx.gui.utils.Icons;
import jadx.gui.utils.JumpPosition;
import jadx.gui.utils.NLS;
import jadx.gui.utils.OverlayIcon;
import jadx.gui.utils.UiUtils;

public class TabbedPane implements ITabStatesListener, DockingListener {
	private static final Logger LOG = LoggerFactory.getLogger(TabbedPane.class);
	private static final int TAB_TITLE_MAX_LENGTH = 30;
	private static final String ID_PREFIX = "editor:";

	private final transient MainWindow mainWindow;
	private final transient TabsController controller;
	private final DockPanel anchor;
	private final Map<ContentPanel, EditorDock> docks = new IdentityHashMap<>();
	private final Map<JNode, List<ContentPanel>> byNode = new LinkedHashMap<>();
	private final List<ChangeListener> changeListeners = new ArrayList<>();
	private final SplitSync splitSync = new SplitSync();

	private @Nullable ContentPanel curTab;
	private @Nullable ContentPanel lastTab;
	private boolean dragging;
	private boolean bulk;
	private boolean updating;
	private int nextId;

	public TabbedPane(MainWindow window, TabsController controller, DockPanel anchor) {
		this.mainWindow = window;
		this.controller = controller;
		this.anchor = anchor;
		controller.addListener(this);
		Docking.addDockingListener(this);
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("focusOwner", ev -> {
			if (ev.getNewValue() instanceof Component) {
				onFocus((Component) ev.getNewValue());
			}
		});
		interceptTabKey();
		interceptCloseKey();
	}

	public MainWindow getMainWindow() {
		return mainWindow;
	}

	public TabsController getTabsController() {
		return controller;
	}

	public void addChangeListener(ChangeListener listener) {
		changeListeners.add(listener);
	}

	public void removeChangeListener(ChangeListener listener) {
		changeListeners.remove(listener);
	}

	private void fireStateChanged() {
		ChangeEvent event = new ChangeEvent(this);
		for (ChangeListener l : new ArrayList<>(changeListeners)) {
			l.stateChanged(event);
		}
	}

	private void interceptTabKey() {
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
			private static final int ctrlDown = KeyEvent.CTRL_DOWN_MASK;
			private long ctrlInterval = 0;

			@Override
			public boolean dispatchKeyEvent(KeyEvent e) {
				long cur = System.currentTimeMillis();
				if (!FocusManager.isActive()) {
					return false;
				}
				int code = e.getKeyCode();
				boolean consume = code == KeyEvent.VK_TAB;
				boolean isReleased = e.getID() == KeyEvent.KEY_RELEASED;
				if (isReleased) {
					if (code == KeyEvent.VK_CONTROL) {
						ctrlInterval = cur;
					} else if (code == KeyEvent.VK_TAB) {
						boolean doSwitch = false;
						if ((e.getModifiersEx() & ctrlDown) != 0) {
							doSwitch = lastTab != null && docks.size() > 1;
						} else {
							ctrlInterval = cur - ctrlInterval;
							if (ctrlInterval <= 90) {
								doSwitch = lastTab != null && docks.size() > 1;
							}
						}
						if (doSwitch && lastTab != null) {
							selectTab(lastTab);
						}
					}
				} else if (consume && (e.getModifiersEx() & ctrlDown) == 0) {
					if (curTab instanceof ClassCodeContentPanel) {
						((ClassCodeContentPanel) curTab).switchPanel();
					}
				}
				return consume;
			}
		});
	}

	private void interceptCloseKey() {
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
			private static final int closeKey = KeyEvent.VK_W;
			private boolean canClose = true;

			@Override
			public boolean dispatchKeyEvent(KeyEvent e) {
				if (!FocusManager.isActive()) {
					return false;
				}
				if (e.getKeyCode() != closeKey) {
					return false;
				}
				if (e.getID() == KeyEvent.KEY_RELEASED) {
					canClose = true;
					return false;
				}
				if (e.isControlDown() && canClose && curTab != null) {
					closeCodePanel(curTab);
					canClose = false;
					return true;
				}
				return false;
			}
		});
	}

	private @Nullable ContentPanel showCode(JumpPosition jumpPos) {
		UiUtils.uiThreadGuard();
		JNode jumpNode = jumpPos.getNode();
		ContentPanel contentPanel = getTabByNode(jumpNode);
		if (contentPanel == null) {
			return null;
		}
		selectTab(contentPanel);
		int pos = jumpPos.getPos();
		if (pos <= 0) {
			LOG.warn("Invalid jump: {}", jumpPos, new JadxRuntimeException());
			pos = Math.max(0, jumpNode.getPos());
		}
		contentPanel.scrollToPos(pos);
		return contentPanel;
	}

	public void selectTab(ContentPanel contentPanel) {
		if (curTab != contentPanel && docks.containsKey(contentPanel)) {
			activate(contentPanel);
		}
		controller.selectTab(contentPanel.getNode());
	}

	private void smaliJump(JClass cls, int pos, boolean debugMode) {
		ContentPanel panel = getTabByNode(cls);
		if (panel == null) {
			panel = showCode(new JumpPosition(cls, 1));
			if (panel == null) {
				throw new JadxRuntimeException("Failed to open panel for JClass: " + cls);
			}
		} else {
			selectTab(panel);
		}
		ClassCodeContentPanel codePane = (ClassCodeContentPanel) panel;
		codePane.showSmaliPane();
		SmaliArea smaliArea = (SmaliArea) codePane.getSmaliCodeArea();
		if (debugMode) {
			smaliArea.scrollToDebugPos(pos);
		}
		smaliArea.scrollToPos(pos);
		smaliArea.requestFocus();
	}

	public @Nullable JumpPosition getCurrentPosition() {
		ContentPanel selectedCodePanel = getSelectedContentPanel();
		if (selectedCodePanel instanceof AbstractCodeContentPanel) {
			AbstractCodeArea codeArea = ((AbstractCodeContentPanel) selectedCodePanel).getCodeArea();
			if (codeArea != null) {
				return codeArea.getCurrentPosition();
			}
		}
		return null;
	}

	public void closeCodePanel(ContentPanel contentPanel) {
		closeCodePanel(contentPanel, false);
	}

	public void closeCodePanel(ContentPanel contentPanel, boolean considerPins) {
		List<ContentPanel> panels = byNode.get(contentPanel.getNode());
		if (panels != null && panels.size() > 1) {
			removePanel(contentPanel);
			return;
		}
		controller.closeTab(contentPanel.getNode(), considerPins);
	}

	/**
	 * Open panels in tab order of their groups.
	 */
	public List<ContentPanel> getTabs() {
		List<ContentPanel> ordered = new ArrayList<>();
		Set<JTabbedPane> seen = new HashSet<>();
		for (EditorDock dock : docks.values()) {
			JTabbedPane tabs = dock.enclosingTabs();
			if (tabs == null || !seen.add(tabs)) {
				continue;
			}
			for (int i = 0; i < tabs.getTabCount(); i++) {
				EditorDock d = dockAt(tabs, i);
				if (d != null) {
					ordered.add(d.getPanel());
				}
			}
		}
		for (ContentPanel panel : docks.keySet()) {
			if (!ordered.contains(panel)) {
				ordered.add(panel);
			}
		}
		return ordered;
	}

	private @Nullable EditorDock dockAt(JTabbedPane tabs, int index) {
		Component c = tabs.getComponentAt(index);
		for (EditorDock dock : docks.values()) {
			if (dock.getParent() == c) {
				return dock;
			}
		}
		return null;
	}

	public @Nullable ContentPanel getTabByNode(JNode node) {
		List<ContentPanel> panels = byNode.get(node);
		if (panels == null || panels.isEmpty()) {
			return null;
		}
		if (curTab != null && panels.contains(curTab)) {
			return curTab;
		}
		return panels.get(0);
	}

	public boolean tabWithTitleExists(String tabTitle) {
		for (EditorDock dock : docks.values()) {
			if (dock.getTabText().equals(tabTitle)) {
				return true;
			}
		}
		return false;
	}

	public void refresh(JNode node) {
		List<ContentPanel> panels = byNode.get(node);
		if (panels == null) {
			return;
		}
		for (ContentPanel panel : panels) {
			updateTab(panel);
		}
		fireStateChanged();
	}

	public void reloadInactiveTabs() {
		UiUtils.uiThreadGuard();
		if (docks.size() <= 1) {
			return;
		}
		for (ContentPanel oldPanel : new ArrayList<>(docks.keySet())) {
			if (oldPanel == curTab) {
				continue;
			}
			TabBlueprint tab = controller.getTabByNode(oldPanel.getNode());
			if (tab == null) {
				continue;
			}
			EditorViewState viewState = controller.getEditorViewState(tab);
			JNode node = oldPanel.getNode();
			ContentPanel panel = node.getContentPanel(this);
			if (panel == null) {
				continue;
			}
			EditorDock dock = docks.remove(oldPanel);
			replaceInNode(node, oldPanel, panel);
			oldPanel.dispose();
			FocusManager.listen(panel);
			dock.setContent(panel);
			docks.put(panel, replaceDock(dock, panel));
			controller.restoreEditorViewState(viewState);
		}
		fireStateChanged();
	}

	private EditorDock replaceDock(EditorDock old, ContentPanel panel) {
		EditorDock dock = new EditorDock(old.getPersistentID() + "r", panel);
		dock.setMenuBuilder(menu -> buildMenu(menu, panel));
		dock.setCloseRequest(() -> closeCodePanel(panel, true));
		boolean docked = Docking.isDocked(old);
		if (docked) {
			Docking.dock(dock, old, DockingRegion.CENTER);
			Docking.undock(old);
		}
		Docking.deregisterDockable(old);
		return dock;
	}

	private void replaceInNode(JNode node, ContentPanel oldPanel, ContentPanel panel) {
		List<ContentPanel> panels = byNode.get(node);
		if (panels != null) {
			int i = panels.indexOf(oldPanel);
			if (i >= 0) {
				panels.set(i, panel);
			}
		}
		if (curTab == oldPanel) {
			curTab = panel;
		}
		if (lastTab == oldPanel) {
			lastTab = panel;
		}
	}

	public @Nullable ContentPanel getSelectedContentPanel() {
		return curTab;
	}

	public void closeAllTabs() {
		closeAllTabs(false);
	}

	public void closeAllTabs(boolean considerPins) {
		for (ContentPanel panel : getTabs()) {
			closeCodePanel(panel, considerPins);
		}
	}

	public void loadSettings() {
		for (ContentPanel panel : docks.keySet()) {
			panel.loadSettings();
			updateTab(panel);
		}
	}

	public void reset() {
		closeAllTabs();
		for (ContentPanel panel : new ArrayList<>(docks.keySet())) {
			removePanel(panel);
		}
		byNode.clear();
		curTab = null;
		lastTab = null;
		FocusManager.reset();
	}

	@Nullable
	public Component getFocusedComp() {
		return FocusManager.getFocusedComp();
	}

	public void split(ContentPanel panel, DockingRegion region) {
		EditorDock source = docks.get(panel);
		if (source == null) {
			return;
		}
		JNode node = panel.getNode();
		ContentPanel copy = node.getContentPanel(this);
		if (copy == null) {
			return;
		}
		controller.setTabPreview(node, false);
		if (panel instanceof ClassCodeContentPanel && copy instanceof ClassCodeContentPanel) {
			((ClassCodeContentPanel) copy).showPaneLike((ClassCodeContentPanel) panel);
		}
		addPanel(copy, source, region);
		splitSync.link(panel, copy);
		activate(copy);
	}

	private EditorDock addPanel(ContentPanel panel, @Nullable EditorDock near, DockingRegion region) {
		FocusManager.listen(panel);
		EditorDock dock = new EditorDock(ID_PREFIX + nextId++, panel);
		dock.setMenuBuilder(menu -> buildMenu(menu, panel));
		dock.setCloseRequest(() -> closeCodePanel(panel, true));
		docks.put(panel, dock);
		byNode.computeIfAbsent(panel.getNode(), n -> new ArrayList<>()).add(panel);
		EditorDock current = curTab == null ? null : docks.get(curTab);
		updating = true;
		try {
			if (near != null && isLive(near)) {
				Docking.dock(dock, near, region);
			} else if (current != null && isLive(current)) {
				Docking.dock(dock, current, DockingRegion.CENTER);
			} else {
				Docking.dock(dock, anchor, DockingRegion.CENTER);
			}
		} finally {
			updating = false;
		}
		installTabMouseListener(dock);
		updateTab(panel);
		return dock;
	}

	private static boolean isLive(EditorDock dock) {
		return Docking.isDockableRegistered(dock.getPersistentID()) && Docking.isDocked(dock);
	}

	private void removePanel(ContentPanel panel) {
		EditorDock dock = docks.remove(panel);
		if (dock == null) {
			return;
		}
		splitSync.unlink(panel);
		List<ContentPanel> panels = byNode.get(panel.getNode());
		if (panels != null) {
			panels.remove(panel);
			if (panels.isEmpty()) {
				byNode.remove(panel.getNode());
			}
		}
		if (curTab == panel) {
			curTab = lastTab != null && docks.containsKey(lastTab) ? lastTab : null;
			lastTab = null;
			if (curTab == null) {
				controller.deselectTab();
			} else {
				controller.selectTab(curTab.getNode());
			}
		} else if (lastTab == panel) {
			lastTab = null;
		}
		bulk = true;
		updating = true;
		try {
			if (Docking.isDocked(dock)) {
				Docking.undock(dock);
			}
			Docking.deregisterDockable(dock);
		} finally {
			bulk = false;
			updating = false;
		}
		panel.dispose();
		fireStateChanged();
	}

	private void activate(ContentPanel panel) {
		if (curTab == panel) {
			return;
		}
		lastTab = curTab;
		curTab = panel;
		EditorDock dock = docks.get(panel);
		if (dock != null && Docking.isDocked(dock)) {
			Docking.bringToFront(dock);
		}
		FocusManager.focusOnCodePanel(panel);
		fireStateChanged();
	}

	private void onFocus(Component comp) {
		for (Map.Entry<ContentPanel, EditorDock> e : docks.entrySet()) {
			if (SwingUtilities.isDescendingFrom(comp, e.getValue())) {
				ContentPanel panel = e.getKey();
				if (curTab != panel) {
					lastTab = curTab;
					curTab = panel;
					controller.selectTab(panel.getNode());
					fireStateChanged();
				}
				return;
			}
		}
	}

	private void installTabMouseListener(EditorDock dock) {
		SwingUtilities.invokeLater(() -> {
			JTabbedPane tabs = dock.enclosingTabs();
			if (tabs == null || Boolean.TRUE.equals(tabs.getClientProperty("jadx.tabMenu"))) {
				return;
			}
			tabs.putClientProperty("jadx.tabMenu", true);
			tabs.setFocusable(false);
			tabs.addChangeListener(e -> {
				int index = tabs.getSelectedIndex();
				EditorDock selected = index < 0 ? null : dockAt(tabs, index);
				if (selected != null && !dragging && !updating) {
					selectTab(selected.getPanel());
				}
			});
			tabs.addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					int index = tabs.indexAtLocation(e.getX(), e.getY());
					if (index < 0) {
						return;
					}
					EditorDock target = dockAt(tabs, index);
					if (target == null) {
						return;
					}
					if (SwingUtilities.isMiddleMouseButton(e)) {
						closeCodePanel(target.getPanel(), true);
					} else if (SwingUtilities.isRightMouseButton(e)) {
						target.showMenu(tabs, e.getX(), e.getY());
					} else if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
						controller.setTabPreview(target.getPanel().getNode(), false);
					}
				}
			});
		});
	}

	private void updateTab(ContentPanel panel) {
		EditorDock dock = docks.get(panel);
		TabBlueprint blueprint = controller.getTabByNode(panel.getNode());
		if (dock == null || blueprint == null) {
			return;
		}
		JNode node = panel.getNode();
		String title = buildTabTitle(node);
		if (node instanceof JEditableNode && ((JEditableNode) node).isChanged()) {
			title = "*" + title;
		}
		if (blueprint.isPreviewTab()) {
			title = "<html><i>" + title + "</i></html>";
		}
		OverlayIcon icon = new OverlayIcon(node.getIcon());
		if (blueprint.isBookmarked()) {
			icon.add(Icons.BOOKMARK_OVERLAY_DARK);
		}
		if (blueprint.isPinned()) {
			icon.add(Icons.PIN);
		}
		dock.update(title, icon, !blueprint.isPinned());
	}

	private String buildTabTitle(JNode node) {
		String tabTitle = node.makeString();
		int count = 0;
		for (ContentPanel other : docks.keySet()) {
			if (other.getNode() != node && other.getNode().makeString().equals(tabTitle)) {
				count++;
			}
		}
		if (count > 0) {
			tabTitle = node.makeLongString();
		}
		return UiUtils.limitStringLength(tabTitle, TAB_TITLE_MAX_LENGTH);
	}

	private void buildMenu(JPopupMenu menu, ContentPanel panel) {
		JNode node = panel.getNode();
		TabBlueprint blueprint = controller.getTabByNode(node);
		if (blueprint == null) {
			return;
		}
		JClass rootClass = node.getRootClass();
		String nodeFullName = rootClass != null ? rootClass.getFullName() : node.getName();
		if (nodeFullName != null) {
			JMenuItem copyRootClassName = new JMenuItem(NLS.str("tabs.copy_class_name"));
			copyRootClassName.addActionListener(ev -> UiUtils.setClipboardString(nodeFullName));
			menu.add(copyRootClassName);
			menu.addSeparator();
		}
		if (node instanceof JClass) {
			JMenuItem splitRight = new JMenuItem(NLS.str("tabs.split_right"));
			splitRight.addActionListener(ev -> split(panel, DockingRegion.EAST));
			menu.add(splitRight);
			JMenuItem splitDown = new JMenuItem(NLS.str("tabs.split_down"));
			splitDown.addActionListener(ev -> split(panel, DockingRegion.SOUTH));
			menu.add(splitDown);
			menu.addSeparator();
		}
		if (blueprint.supportsQuickTabs()) {
			JMenuItem pinTab = new JMenuItem(blueprint.isPinned() ? NLS.str("tabs.unpin") : NLS.str("tabs.pin"));
			pinTab.addActionListener(ev -> {
				boolean pinned = !blueprint.isPinned();
				controller.setTabPinned(node, pinned);
				if (pinned) {
					controller.setTabPositionFirst(node);
				}
			});
			menu.add(pinTab);
			JMenuItem unpinAll = new JMenuItem(NLS.str("tabs.unpin_all"));
			unpinAll.addActionListener(ev -> controller.unpinAllTabs());
			menu.add(unpinAll);
			JMenuItem bookmarkTab = new JMenuItem(blueprint.isBookmarked() ? NLS.str("tabs.unbookmark") : NLS.str("tabs.bookmark"));
			bookmarkTab.addActionListener(ev -> controller.setTabBookmarked(node, !blueprint.isBookmarked()));
			menu.add(bookmarkTab);
			JMenuItem unbookmarkAll = new JMenuItem(NLS.str("tabs.unbookmark_all"));
			unbookmarkAll.addActionListener(ev -> controller.unbookmarkAllTabs());
			menu.add(unbookmarkAll);
			menu.addSeparator();
		}
		if (nodeFullName != null) {
			JadxGuiAction selectInTree = new JadxGuiAction(ActionModel.SYNC, () -> mainWindow.selectNodeInTree(node));
			selectInTree.setShortcut(mainWindow.getShortcutsController().get(ActionModel.SYNC));
			menu.add(selectInTree);
			menu.addSeparator();
		}
		JMenuItem closeTab = new JMenuItem(NLS.str("tabs.close"));
		closeTab.addActionListener(ev -> closeCodePanel(panel, true));
		closeTab.setEnabled(!blueprint.isPinned());
		menu.add(closeTab);

		List<ContentPanel> group = groupOf(panel);
		if (docks.size() > 1) {
			JMenuItem closeOther = new JMenuItem(NLS.str("tabs.closeOthers"));
			closeOther.addActionListener(ev -> {
				for (ContentPanel other : getTabs()) {
					if (other != panel) {
						closeCodePanel(other, true);
					}
				}
			});
			menu.add(closeOther);
			JMenuItem closeAll = new JMenuItem(NLS.str("tabs.closeAll"));
			closeAll.addActionListener(ev -> closeAllTabs(true));
			menu.add(closeAll);
			int currentIndex = group.indexOf(panel);
			if (currentIndex > 0) {
				JMenuItem closeAllLeft = new JMenuItem(NLS.str("tabs.closeAllLeft"));
				closeAllLeft.addActionListener(ev -> {
					for (int i = currentIndex - 1; i >= 0; i--) {
						closeCodePanel(group.get(i), true);
					}
				});
				menu.add(closeAllLeft);
			}
			if (panel != ListUtils.last(group)) {
				JMenuItem closeAllRight = new JMenuItem(NLS.str("tabs.closeAllRight"));
				closeAllRight.addActionListener(ev -> {
					for (int i = group.size() - 1; i > currentIndex; i--) {
						closeCodePanel(group.get(i), true);
					}
				});
				menu.add(closeAllRight);
			}
			menu.addSeparator();
			for (ContentPanel other : getTabs()) {
				if (other == panel) {
					continue;
				}
				JNode otherNode = other.getNode();
				JMenuItem item = new JMenuItem(otherNode.makeLongString());
				item.addActionListener(ev -> selectTab(other));
				item.setIcon(otherNode.getIcon());
				menu.add(item);
			}
		}
	}

	private List<ContentPanel> groupOf(ContentPanel panel) {
		EditorDock dock = docks.get(panel);
		JTabbedPane tabs = dock == null ? null : dock.enclosingTabs();
		List<ContentPanel> group = new ArrayList<>();
		if (tabs == null) {
			group.add(panel);
			return group;
		}
		for (int i = 0; i < tabs.getTabCount(); i++) {
			EditorDock d = dockAt(tabs, i);
			if (d != null) {
				group.add(d.getPanel());
			}
		}
		return group;
	}

	@Override
	public void dockingChange(DockingEvent event) {
		if (event.isTemporary()) {
			dragging = event.getID() == DockingEvent.ID.UNDOCKED;
			return;
		}
		if (event.getID() == DockingEvent.ID.DOCKED) {
			dragging = false;
			for (EditorDock dock : docks.values()) {
				if (dock == event.getDockable()) {
					installTabMouseListener(dock);
					updateTab(dock.getPanel());
				}
			}
			return;
		}
		if (event.getID() != DockingEvent.ID.UNDOCKED || bulk) {
			return;
		}
		for (Map.Entry<ContentPanel, EditorDock> e : docks.entrySet()) {
			if (e.getValue() == event.getDockable()) {
				ContentPanel panel = e.getKey();
				SwingUtilities.invokeLater(() -> {
					if (docks.containsKey(panel)) {
						closeCodePanel(panel, false);
					}
				});
				return;
			}
		}
	}

	/**
	 * Detach every editor from the layout without closing it, e.g. before persisting the layout.
	 */
	public void undockAll() {
		bulk = true;
		updating = true;
		try {
			for (EditorDock dock : docks.values()) {
				if (Docking.isDocked(dock)) {
					Docking.undock(dock);
				}
			}
		} finally {
			bulk = false;
			updating = false;
		}
	}

	public void dockAll() {
		bulk = true;
		updating = true;
		try {
			for (ContentPanel panel : getTabs()) {
				Docking.dock(docks.get(panel), anchor, DockingRegion.CENTER);
			}
			if (curTab != null && docks.get(curTab) != null) {
				Docking.bringToFront(docks.get(curTab));
			}
		} finally {
			bulk = false;
			updating = false;
		}
	}

	@Override
	public void onTabOpen(TabBlueprint blueprint) {
		if (blueprint.isHidden()) {
			return;
		}
		JNode node = blueprint.getNode();
		if (byNode.containsKey(node)) {
			blueprint.setCreated(true);
			return;
		}
		ContentPanel newPanel = node.getContentPanel(this);
		if (newPanel != null) {
			if (node != newPanel.getNode()) {
				throw new JadxRuntimeException("Incorrect node found in content panel");
			}
			addPanel(newPanel, null, DockingRegion.CENTER);
			blueprint.setCreated(true);
		}
	}

	@Override
	public void onTabSelect(TabBlueprint blueprint) {
		ContentPanel contentPanel = getTabByNode(blueprint.getNode());
		if (contentPanel != null && !dragging) {
			activate(contentPanel);
		}
	}

	@Override
	public void onTabCodeJump(TabBlueprint blueprint, @Nullable JumpPosition prevPos, JumpPosition position) {
		mainWindow.getBackgroundExecutor().execute(new SilentTask(() -> UiUtils.uiRun(() -> showCode(position))));
	}

	@Override
	public void onTabSmaliJump(TabBlueprint blueprint, int pos, boolean debugMode) {
		JNode node = blueprint.getNode();
		if (node instanceof JClass) {
			smaliJump((JClass) node, pos, debugMode);
		}
	}

	@Override
	public void onTabClose(TabBlueprint blueprint) {
		List<ContentPanel> panels = byNode.get(blueprint.getNode());
		if (panels == null) {
			return;
		}
		for (ContentPanel panel : new ArrayList<>(panels)) {
			removePanel(panel);
		}
	}

	@Override
	public void onTabPositionFirst(TabBlueprint blueprint) {
		ContentPanel panel = getTabByNode(blueprint.getNode());
		EditorDock dock = panel == null ? null : docks.get(panel);
		JTabbedPane tabs = dock == null ? null : dock.enclosingTabs();
		if (tabs == null) {
			return;
		}
		Component display = dock.getParent();
		int index = tabs.indexOfComponent(display);
		if (index <= 0) {
			return;
		}
		String title = tabs.getTitleAt(index);
		javax.swing.Icon icon = tabs.getIconAt(index);
		tabs.removeTabAt(index);
		tabs.insertTab(title, icon, display, null, 0);
		tabs.setSelectedIndex(0);
	}

	@Override
	public void onTabPinChange(TabBlueprint blueprint) {
		refresh(blueprint.getNode());
	}

	@Override
	public void onTabBookmarkChange(TabBlueprint blueprint) {
		refresh(blueprint.getNode());
	}

	@Override
	public void onTabVisibilityChange(TabBlueprint blueprint) {
		if (!blueprint.isHidden() && !byNode.containsKey(blueprint.getNode())) {
			onTabOpen(blueprint);
		}
		if (blueprint.isHidden() && byNode.containsKey(blueprint.getNode())) {
			onTabClose(blueprint);
		}
	}

	@Override
	public void onTabPreviewChange(TabBlueprint blueprint) {
		refresh(blueprint.getNode());
	}

	@Override
	public void onTabRestore(TabBlueprint blueprint, EditorViewState viewState) {
		ContentPanel contentPanel = getTabByNode(blueprint.getNode());
		if (contentPanel instanceof IViewStateSupport) {
			((IViewStateSupport) contentPanel).restoreEditorViewState(viewState);
		}
	}

	@Override
	public void onTabsReorder(List<TabBlueprint> blueprints) {
		List<TabBlueprint> newBlueprints = new ArrayList<>(blueprints.size());
		for (ContentPanel contentPanel : getTabs()) {
			TabBlueprint blueprint = controller.getTabByNode(contentPanel.getNode());
			if (blueprint != null && !newBlueprints.contains(blueprint)) {
				newBlueprints.add(blueprint);
			}
		}
		Set<TabBlueprint> set = new LinkedHashSet<>(blueprints);
		newBlueprints.forEach(set::remove);
		newBlueprints.addAll(set);
		blueprints.clear();
		blueprints.addAll(newBlueprints);
	}

	@Override
	public void onTabSave(TabBlueprint blueprint, EditorViewState viewState) {
		ContentPanel contentPanel = getTabByNode(blueprint.getNode());
		if (contentPanel instanceof IViewStateSupport) {
			((IViewStateSupport) contentPanel).saveEditorViewState(viewState);
		}
	}

	private static class FocusManager implements FocusListener {
		private static final FocusManager INSTANCE = new FocusManager();
		private static @Nullable Component focusedComp;

		static boolean isActive() {
			return focusedComp != null;
		}

		static void reset() {
			focusedComp = null;
		}

		static Component getFocusedComp() {
			return focusedComp;
		}

		@Override
		public void focusGained(FocusEvent e) {
			focusedComp = (Component) e.getSource();
		}

		@Override
		public void focusLost(FocusEvent e) {
			focusedComp = null;
		}

		static void listen(ContentPanel pane) {
			if (pane instanceof ClassCodeContentPanel) {
				((ClassCodeContentPanel) pane).getCodeArea().addFocusListener(INSTANCE);
				((ClassCodeContentPanel) pane).getSmaliCodeArea().addFocusListener(INSTANCE);
				return;
			}
			if (pane instanceof AbstractCodeContentPanel) {
				((AbstractCodeContentPanel) pane).getChildrenComponent().addFocusListener(INSTANCE);
				return;
			}
			if (pane instanceof HtmlPanel) {
				((HtmlPanel) pane).getHtmlArea().addFocusListener(INSTANCE);
				return;
			}
			if (pane instanceof ImagePanel || pane instanceof FontPanel) {
				pane.addFocusListener(INSTANCE);
			}
		}

		static void focusOnCodePanel(ContentPanel pane) {
			if (pane instanceof ClassCodeContentPanel) {
				SwingUtilities.invokeLater(() -> ((ClassCodeContentPanel) pane).getCurrentCodeArea().requestFocus());
				return;
			}
			if (pane instanceof AbstractCodeContentPanel) {
				SwingUtilities.invokeLater(() -> ((AbstractCodeContentPanel) pane).getChildrenComponent().requestFocus());
				return;
			}
			if (pane instanceof HtmlPanel) {
				SwingUtilities.invokeLater(() -> ((HtmlPanel) pane).getHtmlArea().requestFocusInWindow());
				return;
			}
			if (pane instanceof ImagePanel || pane instanceof FontPanel) {
				SwingUtilities.invokeLater(pane::requestFocusInWindow);
			}
		}
	}
}
