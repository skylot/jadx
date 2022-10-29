package jadx.gui.ui;

import java.awt.Component;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.codearea.AbstractCodeArea;
import jadx.gui.ui.codearea.AbstractCodeContentPanel;
import jadx.gui.ui.codearea.ClassCodeContentPanel;
import jadx.gui.ui.codearea.EditorViewState;
import jadx.gui.ui.codearea.SmaliArea;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.ui.panel.HtmlPanel;
import jadx.gui.ui.panel.IViewStateSupport;
import jadx.gui.ui.panel.ImagePanel;
import jadx.gui.utils.JumpManager;
import jadx.gui.utils.JumpPosition;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

public class TabbedPane extends JTabbedPane {
	private static final long serialVersionUID = -8833600618794570904L;

	private static final Logger LOG = LoggerFactory.getLogger(TabbedPane.class);

	private final transient MainWindow mainWindow;
	private final transient Map<JNode, ContentPanel> openTabs = new LinkedHashMap<>();
	private final transient JumpManager jumps = new JumpManager();

	private transient ContentPanel curTab;
	private transient ContentPanel lastTab;

	TabbedPane(MainWindow window) {
		this.mainWindow = window;

		setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

		addMouseWheelListener(event -> {
			int direction = event.getWheelRotation();
			if (openTabs.isEmpty() || direction == 0) {
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
			ContentPanel tab = getSelectedCodePanel();
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

	/**
	 * Jump to node definition
	 */
	public void codeJump(JNode node) {
		if (node.getPos() != 0 || node.getRootClass() == null) {
			codeJump(new JumpPosition(node));
			return;
		}
		// node need loading
		mainWindow.getBackgroundExecutor().execute(
				NLS.str("progress.load"),
				() -> node.getRootClass().getCodeInfo(), // run heavy loading in background
				status -> codeJump(new JumpPosition(node)));
	}

	/**
	 * Prefer {@link TabbedPane#codeJump(JNode)} method
	 */
	public void codeJump(JumpPosition pos) {
		saveJump(pos);
		showCode(pos);
	}

	private void saveJump(JumpPosition pos) {
		JumpPosition curPos = getCurrentPosition();
		if (curPos != null) {
			jumps.addPosition(curPos);
			jumps.addPosition(pos);
		}
	}

	private void showCode(JumpPosition jumpPos) {
		ContentPanel contentPanel = getContentPanel(jumpPos.getNode());
		if (contentPanel != null) {
			scrollToPos(contentPanel, jumpPos.getPos());
			selectTab(contentPanel);
		}
	}

	public boolean showNode(JNode node) {
		final ContentPanel contentPanel = getContentPanel(node);
		if (contentPanel == null) {
			return false;
		}
		selectTab(contentPanel);
		return true;
	}

	private void scrollToPos(ContentPanel contentPanel, int pos) {
		if (pos == 0) {
			LOG.warn("Ignore zero jump!", new JadxRuntimeException());
			return;
		}
		if (contentPanel instanceof AbstractCodeContentPanel) {
			AbstractCodeArea codeArea = ((AbstractCodeContentPanel) contentPanel).getCodeArea();
			codeArea.scrollToPos(pos);
			codeArea.requestFocus();
		}
	}

	public void selectTab(ContentPanel contentPanel) {
		setSelectedComponent(contentPanel);
		if (mainWindow.getSettings().isAlwaysSelectOpened()) {
			mainWindow.syncWithEditor();
		}
	}

	public void smaliJump(JClass cls, int pos, boolean debugMode) {
		ContentPanel panel = getOpenTabs().get(cls);
		if (panel == null) {
			showCode(new JumpPosition(cls, 1));
			panel = getOpenTabs().get(cls);
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

	@Nullable
	public JumpPosition getCurrentPosition() {
		ContentPanel selectedCodePanel = getSelectedCodePanel();
		if (selectedCodePanel instanceof AbstractCodeContentPanel) {
			return ((AbstractCodeContentPanel) selectedCodePanel).getCodeArea().getCurrentPosition();
		}
		return null;
	}

	public List<EditorViewState> getEditorViewStates() {
		List<EditorViewState> states = new ArrayList<>();
		for (ContentPanel panel : openTabs.values()) {
			if (panel instanceof IViewStateSupport) {
				states.add(((IViewStateSupport) panel).getEditorViewState());
			} else {
				states.add(new EditorViewState(panel.getNode(), "", 0, EditorViewState.ZERO));
			}
		}
		return states;
	}

	public void restoreEditorViewState(EditorViewState viewState) {
		ContentPanel contentPanel = getContentPanel(viewState.getNode());
		if (contentPanel instanceof IViewStateSupport) {
			((IViewStateSupport) contentPanel).restoreEditorViewState(viewState);
		}
	}

	public void navBack() {
		if (jumps.size() > 1) {
			jumps.updateCurPosition(getCurrentPosition());
		}
		JumpPosition pos = jumps.getPrev();
		if (pos != null) {
			showCode(pos);
		}
	}

	public void navForward() {
		if (jumps.size() > 1) {
			jumps.updateCurPosition(getCurrentPosition());
		}
		JumpPosition pos = jumps.getNext();
		if (pos != null) {
			showCode(pos);
		}
	}

	private void addContentPanel(ContentPanel contentPanel) {
		openTabs.put(contentPanel.getNode(), contentPanel);
		int tabCount = getTabCount();
		add(contentPanel, tabCount);
		setTabComponentAt(tabCount, makeTabComponent(contentPanel));
	}

	public void closeCodePanel(ContentPanel contentPanel) {
		openTabs.remove(contentPanel.getNode());
		remove(contentPanel);
		contentPanel.dispose();
	}

	@Nullable
	private ContentPanel getContentPanel(JNode node) {
		ContentPanel panel = openTabs.get(node);
		if (panel == null) {
			panel = node.getContentPanel(this);
			if (panel == null) {
				return null;
			}
			FocusManager.listen(panel);
			addContentPanel(panel);
		}
		return panel;
	}

	public void refresh(JNode node) {
		ContentPanel panel = openTabs.get(node);
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
			JNode node = ((ContentPanel) getComponentAt(i)).getNode();
			ContentPanel panel = node.getContentPanel(this);
			FocusManager.listen(panel);
			openTabs.put(node, panel);
			setComponentAt(i, panel);
			setTabComponentAt(i, makeTabComponent(panel));
		}
		fireStateChanged();
	}

	@Nullable
	public ContentPanel getSelectedCodePanel() {
		return (ContentPanel) getSelectedComponent();
	}

	private Component makeTabComponent(final ContentPanel contentPanel) {
		return new TabComponent(this, contentPanel);
	}

	public void closeAllTabs() {
		List<ContentPanel> contentPanels = new ArrayList<>(openTabs.values());
		for (ContentPanel panel : contentPanels) {
			closeCodePanel(panel);
		}
	}

	public Map<JNode, ContentPanel> getOpenTabs() {
		return openTabs;
	}

	public void loadSettings() {
		for (ContentPanel panel : openTabs.values()) {
			panel.loadSettings();
		}
		int tabCount = getTabCount();
		for (int i = 0; i < tabCount; i++) {
			Component tabComponent = getTabComponentAt(i);
			if (tabComponent instanceof TabComponent) {
				((TabComponent) tabComponent).loadSettings();
			}
		}
	}

	public void reset() {
		closeAllTabs();
		openTabs.clear();
		jumps.reset();
		curTab = null;
		lastTab = null;
		FocusManager.reset();
	}

	@Nullable
	public Component getFocusedComp() {
		return FocusManager.getFocusedComp();
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
				((AbstractCodeContentPanel) pane).getCodeArea().addFocusListener(INSTANCE);
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
			// throw new JadxRuntimeException("Add the new ContentPanel to TabbedPane.FocusManager: " + pane);
		}

		static void focusOnCodePanel(ContentPanel pane) {
			if (pane instanceof ClassCodeContentPanel) {
				SwingUtilities.invokeLater(() -> ((ClassCodeContentPanel) pane).getCurrentCodeArea().requestFocus());
				return;
			}
			if (pane instanceof AbstractCodeContentPanel) {
				SwingUtilities.invokeLater(() -> ((AbstractCodeContentPanel) pane).getCodeArea().requestFocus());
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
			// throw new JadxRuntimeException("Add the new ContentPanel to TabbedPane.FocusManager: " + pane);
		}
	}
}
