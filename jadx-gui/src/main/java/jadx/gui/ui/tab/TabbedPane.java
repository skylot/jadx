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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.AbstractCodeArea;
import jadx.gui.ui.codearea.AbstractCodeContentPanel;
import jadx.gui.ui.codearea.ClassCodeContentPanel;
import jadx.gui.ui.codearea.EditorViewState;
import jadx.gui.ui.codearea.SmaliArea;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.ui.panel.FontPanel;
import jadx.gui.ui.panel.HtmlPanel;
import jadx.gui.ui.panel.IViewStateSupport;
import jadx.gui.ui.panel.ImagePanel;
import jadx.gui.ui.tab.dnd.TabDndController;
import jadx.gui.utils.JumpPosition;
import jadx.gui.utils.UiUtils;

public class TabbedPane extends JTabbedPane implements ITabStatesListener {
	private static final long serialVersionUID = -8833600618794570904L;

	private static final Logger LOG = LoggerFactory.getLogger(TabbedPane.class);

	private final transient MainWindow mainWindow;
	private final transient TabsController controller;
	private final transient Map<JNode, ContentPanel> tabsMap = new HashMap<>();

	private transient ContentPanel curTab;
	private transient ContentPanel lastTab;

	private transient TabDndController dnd;

	public TabbedPane(MainWindow window, TabsController controller) {
		this.mainWindow = window;
		this.controller = controller;

		controller.addListener(this);
		setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

		MouseAdapter clickAdapter = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				int tabIndex = indexAtLocation(e.getX(), e.getY());
				if (tabIndex == -1 || tabIndex > getTabCount()) {
					return;
				}
				TabComponent tab = (TabComponent) getTabComponentAt(tabIndex);
				tab.dispatchEvent(e);
			}
		};
		addMouseListener(clickAdapter);

		addMouseWheelListener(event -> {
			if (dnd != null && dnd.isDragging()) {
				return;
			}
			int direction = event.getWheelRotation();
			if (getTabCount() == 0 || direction == 0) {
				return;
			}
			direction = (direction < 0) ? -1 : 1; // normalize direction
			int index = getSelectedIndex();
			int maxIndex = getTabCount() - 1;
			index += direction;
			// switch between first tab <-> last tab
			if (index < 0) {
				index = maxIndex;
			} else if (index > maxIndex) {
				index = 0;
			}
			try {
				setSelectedIndex(index);
			} catch (IndexOutOfBoundsException e) {
				// ignore error
			}
		});
		interceptTabKey();
		interceptCloseKey();
		enableSwitchingTabs();
	}

	private void interceptTabKey() {
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {
			private static final int ctrlDown = KeyEvent.CTRL_DOWN_MASK;
			private long ctrlInterval = 0;

			@Override
			public boolean dispatchKeyEvent(KeyEvent e) {
				long cur = System.currentTimeMillis();
				if (!FocusManager.isActive()) {
					return false; // don't do nothing when tab is not on focus.
				}
				int code = e.getKeyCode();
				boolean consume = code == KeyEvent.VK_TAB; // consume Tab key event anyway
				boolean isReleased = e.getID() == KeyEvent.KEY_RELEASED;
				if (isReleased) {
					if (code == KeyEvent.VK_CONTROL) {
						ctrlInterval = cur;
					} else if (code == KeyEvent.VK_TAB) {
						boolean doSwitch = false;
						if ((e.getModifiersEx() & ctrlDown) != 0) {
							doSwitch = lastTab != null && getTabCount() > 1;
						} else {
							// the gap of the release of ctrl and tab is very close, nearly the same time,
							// but ctrl released first.
							ctrlInterval = cur - ctrlInterval;
							if (ctrlInterval <= 90) {
								doSwitch = lastTab != null && getTabCount() > 1;
							}
						}
						if (doSwitch) {
							selectTab(lastTab);
						}
					}
				} else if (consume && (e.getModifiersEx() & ctrlDown) == 0) {
					// switch between source and smali
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
					return false; // do nothing when tab is not on focus.
				}
				if (e.getKeyCode() != closeKey) {
					return false; // only intercept the events of the close key
				}
				if (e.getID() == KeyEvent.KEY_RELEASED) {
					canClose = true; // after the close key is lifted we allow to use it again
					return false;
				}
				if (e.isControlDown() && canClose) {
					// close the current tab
					closeCodePanel(curTab);
					canClose = false; // make sure we dont close more tabs until the close key is lifted
					return true;
				}
				return false;
			}
		});
	}

	private void enableSwitchingTabs() {
		addChangeListener(e -> {
			ContentPanel tab = getSelectedContentPanel();
			if (tab == null) { // all closed
				curTab = null;
				lastTab = null;
				return;
			}
			FocusManager.focusOnCodePanel(tab);
			if (tab == curTab) { // a tab was closed by not the current one.
				if (lastTab != null && indexOfComponent(lastTab) == -1) { // lastTab was closed
					setLastTabAdjacentToCurTab();
				}
				return;
			}
			if (tab == lastTab) {
				if (indexOfComponent(curTab) == -1) { // curTab was closed and lastTab is the current one.
					curTab = lastTab;
					setLastTabAdjacentToCurTab();
					return;
				}
				// it's switching between lastTab and curTab.
			}
			lastTab = curTab;
			curTab = tab;
		});
	}

	private void setLastTabAdjacentToCurTab() {
		if (getTabCount() < 2) {
			lastTab = null;
			return;
		}
		int idx = indexOfComponent(curTab);
		if (idx == 0) {
			lastTab = (ContentPanel) getComponentAt(idx + 1);
		} else {
			lastTab = (ContentPanel) getComponentAt(idx - 1);
		}
	}

	public MainWindow getMainWindow() {
		return mainWindow;
	}

	public TabsController getTabsController() {
		return controller;
	}

	private @Nullable ContentPanel showCode(JumpPosition jumpPos) {
		ContentPanel contentPanel = getContentPanel(jumpPos.getNode());
		if (contentPanel != null) {
			selectTab(contentPanel);
			scrollToPos(contentPanel, jumpPos.getPos());
		}
		return contentPanel;
	}

	private void scrollToPos(ContentPanel contentPanel, int pos) {
		if (pos == 0) {
			LOG.warn("Ignore zero jump!", new JadxRuntimeException());
			return;
		}
		if (contentPanel instanceof AbstractCodeContentPanel) {
			AbstractCodeArea codeArea = ((AbstractCodeContentPanel) contentPanel).getCodeArea();
			codeArea.requestFocus();
			codeArea.scrollToPos(pos);
		}
	}

	public void selectTab(ContentPanel contentPanel) {
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
		ClassCodeContentPanel codePane = ((ClassCodeContentPanel) panel);
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
			return ((AbstractCodeContentPanel) selectedCodePanel).getCodeArea().getCurrentPosition();
		}
		return null;
	}

	private void addContentPanel(ContentPanel contentPanel) {
		tabsMap.put(contentPanel.getNode(), contentPanel);
		int tabCount = getTabCount();
		add(contentPanel, tabCount);
		setTabComponentAt(tabCount, makeTabComponent(contentPanel));
	}

	public void closeCodePanel(ContentPanel contentPanel) {
		closeCodePanel(contentPanel, false);
	}

	public void closeCodePanel(ContentPanel contentPanel, boolean considerPins) {
		controller.closeTab(contentPanel.getNode(), considerPins);
	}

	public List<ContentPanel> getTabs() {
		List<ContentPanel> list = new ArrayList<>(getTabCount());
		for (int i = 0; i < getTabCount(); i++) {
			list.add((ContentPanel) getComponentAt(i));
		}
		return list;
	}

	public @Nullable ContentPanel getTabByNode(JNode node) {
		return tabsMap.get(node);
	}

	public @Nullable TabComponent getTabComponentByNode(JNode node) {
		ContentPanel contentPanel = getTabByNode(node);
		if (contentPanel == null) {
			return null;
		}
		int index = indexOfComponent(contentPanel);
		if (index == -1) {
			return null;
		}
		Component component = getTabComponentAt(index);
		if (!(component instanceof TabComponent)) {
			return null;
		}

		return (TabComponent) component;
	}

	private @Nullable ContentPanel getContentPanel(JNode node) {
		controller.openTab(node);
		return getTabByNode(node);
	}

	public void refresh(JNode node) {
		ContentPanel panel = getTabByNode(node);
		if (panel != null) {
			setTabComponentAt(indexOfComponent(panel), makeTabComponent(panel));
			fireStateChanged();
		}
	}

	public void reloadInactiveTabs() {
		UiUtils.uiThreadGuard();
		int tabCount = getTabCount();
		if (tabCount == 1) {
			return;
		}
		int current = getSelectedIndex();
		for (int i = 0; i < tabCount; i++) {
			if (i == current) {
				continue;
			}
			ContentPanel oldPanel = (ContentPanel) getComponentAt(i);
			TabBlueprint tab = controller.getTabByNode(oldPanel.getNode());
			if (tab == null) {
				continue;
			}
			EditorViewState viewState = controller.getEditorViewState(tab);
			JNode node = oldPanel.getNode();
			ContentPanel panel = node.getContentPanel(this);
			FocusManager.listen(panel);
			tabsMap.put(node, panel);
			setComponentAt(i, panel);
			setTabComponentAt(i, makeTabComponent(panel));
			controller.restoreEditorViewState(viewState);
		}
		fireStateChanged();
	}

	@Nullable
	public ContentPanel getSelectedContentPanel() {
		return (ContentPanel) getSelectedComponent();
	}

	private Component makeTabComponent(final ContentPanel contentPanel) {
		return new TabComponent(this, contentPanel);
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
		for (int i = 0; i < getTabCount(); i++) {
			((ContentPanel) getComponentAt(i)).loadSettings();
			((TabComponent) getTabComponentAt(i)).loadSettings();
		}
	}

	public void reset() {
		closeAllTabs();
		tabsMap.clear();
		curTab = null;
		lastTab = null;
		FocusManager.reset();
	}

	@Nullable
	public Component getFocusedComp() {
		return FocusManager.getFocusedComp();
	}

	public TabDndController getDnd() {
		return dnd;
	}

	public void setDnd(TabDndController dnd) {
		this.dnd = dnd;
	}

	@Override
	public void onTabOpen(TabBlueprint blueprint) {
		if (blueprint.isHidden()) {
			return;
		}
		ContentPanel newPanel = blueprint.getNode().getContentPanel(this);
		if (newPanel != null) {
			FocusManager.listen(newPanel);
			addContentPanel(newPanel);
		}
	}

	@Override
	public void onTabSelect(TabBlueprint blueprint) {
		ContentPanel contentPanel = getContentPanel(blueprint.getNode());
		if (contentPanel != null) {
			setSelectedComponent(contentPanel);
		}
	}

	@Override
	public void onTabCodeJump(TabBlueprint blueprint, @Nullable JumpPosition prevPos, JumpPosition position) {
		showCode(position);
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
		ContentPanel contentPanel = getTabByNode(blueprint.getNode());
		if (contentPanel == null) {
			return;
		}
		tabsMap.remove(contentPanel.getNode());
		remove(contentPanel);
		contentPanel.dispose();
	}

	@Override
	public void onTabPositionFirst(TabBlueprint blueprint) {
		ContentPanel contentPanel = getTabByNode(blueprint.getNode());
		if (contentPanel == null) {
			return;
		}
		setTabPosition(contentPanel, 0);
	}

	private void setTabPosition(ContentPanel contentPanel, int position) {
		TabComponent tabComponent = getTabComponentByNode(contentPanel.getNode());
		if (tabComponent == null) {
			return;
		}
		remove(contentPanel);
		add(contentPanel, position);
		setTabComponentAt(position, tabComponent);
	}

	@Override
	public void onTabPinChange(TabBlueprint blueprint) {
		TabComponent tabComponent = getTabComponentByNode(blueprint.getNode());
		if (tabComponent == null) {
			return;
		}
		tabComponent.updateCloseOrPinButton();
	}

	@Override
	public void onTabBookmarkChange(TabBlueprint blueprint) {
		TabComponent tabComponent = getTabComponentByNode(blueprint.getNode());
		if (tabComponent == null) {
			return;
		}
		tabComponent.updateBookmarkIcon();
	}

	@Override
	public void onTabVisibilityChange(TabBlueprint blueprint) {
		if (!blueprint.isHidden() && !tabsMap.containsKey(blueprint.getNode())) {
			onTabOpen(blueprint);
		}
		if (blueprint.isHidden() && tabsMap.containsKey(blueprint.getNode())) {
			onTabClose(blueprint);
		}
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
			if (blueprint != null) {
				newBlueprints.add(blueprint);
			}
		}
		// Add back hidden tabs
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
			if (pane instanceof ImagePanel) {
				pane.addFocusListener(INSTANCE);
				return;
			}
			if (pane instanceof FontPanel) {
				pane.addFocusListener(INSTANCE);
				return;
			}
			// throw new JadxRuntimeException("Add the new ContentPanel to TabbedPane.FocusManager: " + pane);
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
			if (pane instanceof ImagePanel) {
				SwingUtilities.invokeLater(((ImagePanel) pane)::requestFocusInWindow);
				return;
			}
			if (pane instanceof FontPanel) {
				SwingUtilities.invokeLater(((FontPanel) pane)::requestFocusInWindow);
				return;
			}
			// throw new JadxRuntimeException("Add the new ContentPanel to TabbedPane.FocusManager: " + pane);
		}
	}
}
