package jadx.gui.ui.tab;

import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;

public class QuickTabsChildNode extends QuickTabsBaseNode {
	private final JNode node;

	public QuickTabsChildNode(JNode node) {
		this.node = node;
	}

	@Override
	public String toString() {
		return node.toString();
	}

	public JNode getJNode() {
		return node;
	}

	@Override
	public JPopupMenu onTreePopupMenu(MainWindow mainWindow) {
		JPopupMenu menu = node.onTreePopupMenu(mainWindow);

		if (node.supportsQuickTabs()) {
			if (getParent() instanceof QuickTabsPinParentNode) {
				if (menu == null) {
					menu = new JPopupMenu();
				}

				JMenuItem closeAction = new JMenuItem(NLS.str("tabs.close"));
				closeAction.addActionListener(e -> mainWindow.getTabsController().closeTab(node, true));
				menu.add(closeAction, 0);
				menu.add(new JPopupMenu.Separator(), 1);
			}
			if (getParent() instanceof QuickTabsPinParentNode) {
				if (menu == null) {
					menu = new JPopupMenu();
				}

				JMenuItem unpinAction = new JMenuItem(NLS.str("tabs.unpin"));
				unpinAction.addActionListener(e -> mainWindow.getTabsController().setTabPinned(node, false));
				menu.add(unpinAction, 0);
				menu.add(new JPopupMenu.Separator(), 1);
			}
			if (getParent() instanceof QuickTabsBookmarkParentNode) {
				if (menu == null) {
					menu = new JPopupMenu();
				}

				JMenuItem unbookmarkAction = new JMenuItem(NLS.str("tabs.unbookmark"));
				unbookmarkAction.addActionListener(e -> mainWindow.getTabsController().setTabBookmarked(node, false));
				menu.add(unbookmarkAction, 0);
				menu.add(new JPopupMenu.Separator(), 1);
			}
		}

		return menu;
	}

	@Override
	Icon getIcon() {
		return node.getIcon();
	}
}
