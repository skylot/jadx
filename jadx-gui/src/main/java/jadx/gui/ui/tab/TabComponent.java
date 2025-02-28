package jadx.gui.ui.tab;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicButtonUI;

import jadx.core.utils.ListUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JEditableNode;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.action.ActionModel;
import jadx.gui.ui.action.JadxGuiAction;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.ui.tab.dnd.TabDndGestureListener;
import jadx.gui.utils.Icons;
import jadx.gui.utils.NLS;
import jadx.gui.utils.OverlayIcon;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.ui.NodeLabel;

public class TabComponent extends JPanel {
	private static final long serialVersionUID = -8147035487543610321L;

	private final TabbedPane tabbedPane;
	private final TabsController tabsController;
	private final ContentPanel contentPanel;

	private OverlayIcon icon;
	private JLabel label;
	private JButton pinBtn;
	private JButton closeBtn;

	public TabComponent(TabbedPane tabbedPane, ContentPanel contentPanel) {
		this.tabbedPane = tabbedPane;
		this.tabsController = tabbedPane.getMainWindow().getTabsController();
		this.contentPanel = contentPanel;

		init();
	}

	public void loadSettings() {
		label.setFont(getLabelFont());
		if (tabbedPane.getDnd() != null) {
			tabbedPane.getDnd().loadSettings();
		}
	}

	private Font getLabelFont() {
		return tabsController.getMainWindow().getSettings().getFont().deriveFont(Font.BOLD);
	}

	private void init() {
		setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
		setOpaque(false);

		JNode node = getNode();
		icon = new OverlayIcon(node.getIcon());

		label = new NodeLabel(buildTabTitle(node), node.disableHtml());
		label.setFont(getLabelFont());
		String toolTip = contentPanel.getTabTooltip();
		if (toolTip != null) {
			setToolTipText(toolTip);
		}
		label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
		label.setIcon(icon);
		updateBookmarkIcon();
		if (node instanceof JEditableNode) {
			((JEditableNode) node).addChangeListener(c -> label.setText(buildTabTitle(node)));
		}

		pinBtn = new JButton();
		pinBtn.setIcon(Icons.PIN);
		pinBtn.setRolloverIcon(Icons.PIN_HOVERED);
		pinBtn.setRolloverEnabled(true);
		pinBtn.setOpaque(false);
		pinBtn.setUI(new BasicButtonUI());
		pinBtn.setContentAreaFilled(false);
		pinBtn.setBorder(null);
		pinBtn.setBorderPainted(false);
		pinBtn.addActionListener(e -> togglePin());

		closeBtn = new JButton();
		closeBtn.setIcon(Icons.CLOSE_INACTIVE);
		closeBtn.setRolloverIcon(Icons.CLOSE);
		closeBtn.setRolloverEnabled(true);
		closeBtn.setOpaque(false);
		closeBtn.setUI(new BasicButtonUI());
		closeBtn.setContentAreaFilled(false);
		closeBtn.setFocusable(false);
		closeBtn.setBorder(null);
		closeBtn.setBorderPainted(false);
		closeBtn.addActionListener(e -> {
			tabsController.closeTab(node, true);
		});

		MouseAdapter clickAdapter = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isMiddleMouseButton(e)) {
					tabsController.closeTab(node, true);
				} else if (SwingUtilities.isRightMouseButton(e)) {
					JPopupMenu menu = createTabPopupMenu();
					menu.show(e.getComponent(), e.getX(), e.getY());
				} else if (SwingUtilities.isLeftMouseButton(e)) {
					tabsController.selectTab(node);
				}
			}
		};
		addMouseListener(clickAdapter);
		addListenerForDnd();

		add(label);
		updateCloseOrPinButton();
		setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
	}

	public void updateCloseOrPinButton() {
		if (getBlueprint().isPinned()) {
			if (closeBtn.isShowing()) {
				remove(closeBtn);
			}
			if (!pinBtn.isShowing()) {
				add(pinBtn);
			}
		} else {
			if (pinBtn.isShowing()) {
				remove(pinBtn);
			}
			if (!closeBtn.isShowing()) {
				add(closeBtn);
			}
		}
	}

	public void updateBookmarkIcon() {
		icon.clear();

		if (getBlueprint().isBookmarked()) {
			icon.add(Icons.BOOKMARK_OVERLAY_DARK);
		}
		label.repaint();
	}

	private void togglePin() {
		boolean pinned = !getBlueprint().isPinned();
		tabsController.setTabPinned(getNode(), pinned);

		if (pinned) {
			tabsController.setTabPositionFirst(getNode());
		}
	}

	private void toggleBookmark() {
		boolean bookmarked = !getBlueprint().isBookmarked();
		tabsController.setTabBookmarked(getNode(), bookmarked);
	}

	private void addListenerForDnd() {
		if (tabbedPane.getDnd() == null) {
			return;
		}
		TabComponent comp = this;
		DragGestureListener dgl = new TabDndGestureListener(tabbedPane.getDnd()) {
			@Override
			protected Point getDragOrigin(DragGestureEvent e) {
				return SwingUtilities.convertPoint(comp, e.getDragOrigin(), tabbedPane);
			}
		};
		DragSource.getDefaultDragSource()
				.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY_OR_MOVE, dgl);
	}

	private String buildTabTitle(JNode node) {
		String tabTitle;
		if (node.getRootClass() != null) {
			tabTitle = node.getRootClass().getName();
		} else {
			tabTitle = node.makeLongStringHtml();
		}
		if (node instanceof JEditableNode) {
			if (((JEditableNode) node).isChanged()) {
				return "*" + tabTitle;
			}
		}
		return tabTitle;
	}

	private JPopupMenu createTabPopupMenu() {
		JPopupMenu menu = new JPopupMenu();

		String nodeFullName = getNodeFullName(contentPanel);
		if (nodeFullName != null) {
			JMenuItem copyRootClassName = new JMenuItem(NLS.str("tabs.copy_class_name"));
			copyRootClassName.addActionListener(actionEvent -> UiUtils.setClipboardString(nodeFullName));
			menu.add(copyRootClassName);
			menu.addSeparator();
		}

		if (getBlueprint().supportsQuickTabs()) {
			String pinTitle = getBlueprint().isPinned() ? NLS.str("tabs.unpin") : NLS.str("tabs.pin");
			JMenuItem pinTab = new JMenuItem(pinTitle);
			pinTab.addActionListener(e -> togglePin());
			menu.add(pinTab);

			JMenuItem unpinAll = new JMenuItem(NLS.str("tabs.unpin_all"));
			unpinAll.addActionListener(e -> tabsController.unpinAllTabs());
			menu.add(unpinAll);

			String bookmarkTitle = getBlueprint().isBookmarked() ? NLS.str("tabs.unbookmark") : NLS.str("tabs.bookmark");
			JMenuItem bookmarkTab = new JMenuItem(bookmarkTitle);
			bookmarkTab.addActionListener(e -> toggleBookmark());
			menu.add(bookmarkTab);

			JMenuItem unbookmarkAll = new JMenuItem(NLS.str("tabs.unbookmark_all"));
			unbookmarkAll.addActionListener(e -> tabsController.unbookmarkAllTabs());
			menu.add(unbookmarkAll);
			menu.addSeparator();
		}

		if (nodeFullName != null) {
			MainWindow mainWindow = tabsController.getMainWindow();
			JadxGuiAction selectInTree = new JadxGuiAction(ActionModel.SYNC, () -> mainWindow.selectNodeInTree(getNode()));
			// attach shortcut without bind only to show current keybinding
			selectInTree.setShortcut(mainWindow.getShortcutsController().get(ActionModel.SYNC));
			menu.add(selectInTree);
			menu.addSeparator();
		}

		JMenuItem closeTab = new JMenuItem(NLS.str("tabs.close"));
		closeTab.addActionListener(e -> tabsController.closeTab(getNode(), true));
		if (getBlueprint().isPinned()) {
			closeTab.setEnabled(false);
		}
		menu.add(closeTab);

		List<TabBlueprint> tabs = tabsController.getOpenTabs();
		if (tabs.size() > 1) {
			JMenuItem closeOther = new JMenuItem(NLS.str("tabs.closeOthers"));
			closeOther.addActionListener(e -> {
				JNode currentNode = getNode();
				for (TabBlueprint tab : tabs) {
					if (tab.getNode() != currentNode) {
						tabsController.closeTab(tab, true);
					}
				}
			});
			menu.add(closeOther);

			JMenuItem closeAll = new JMenuItem(NLS.str("tabs.closeAll"));
			closeAll.addActionListener(e -> tabsController.closeAllTabs(true));
			menu.add(closeAll);

			// We don't use TabsController here because tabs position is
			// specific to TabbedPane
			List<ContentPanel> contentPanels = tabbedPane.getTabs();
			if (contentPanel != ListUtils.last(contentPanels)) {
				JMenuItem closeAllRight = new JMenuItem(NLS.str("tabs.closeAllRight"));
				closeAllRight.addActionListener(e -> {
					boolean pastCurrentPanel = false;
					for (ContentPanel panel : contentPanels) {
						if (!pastCurrentPanel) {
							if (panel == contentPanel) {
								pastCurrentPanel = true;
							}
						} else {
							tabsController.closeTab(panel.getNode(), true);
						}
					}
				});
				menu.add(closeAllRight);
			}
			menu.addSeparator();

			TabBlueprint selectedTab = tabsController.getSelectedTab();
			for (TabBlueprint tab : tabs) {
				if (tab == selectedTab) {
					continue;
				}
				JNode node = tab.getNode();
				final String clsName = node.makeLongString();
				JMenuItem item = new JMenuItem(clsName);
				item.addActionListener(e -> tabsController.codeJump(node));
				item.setIcon(node.getIcon());
				menu.add(item);
			}
		}
		return menu;
	}

	private String getNodeFullName(ContentPanel contentPanel) {
		JNode node = contentPanel.getNode();
		JClass jClass = node.getRootClass();
		if (jClass != null) {
			return jClass.getFullName();
		}
		return node.getName();
	}

	public ContentPanel getContentPanel() {
		return contentPanel;
	}

	public TabBlueprint getBlueprint() {
		TabBlueprint blueprint = tabsController.getTabByNode(contentPanel.getNode());
		if (blueprint == null) {
			throw new JadxRuntimeException("TabComponent does not have a corresponding TabBlueprint");
		}
		return blueprint;
	}

	public JNode getNode() {
		return contentPanel.getNode();
	}
}
