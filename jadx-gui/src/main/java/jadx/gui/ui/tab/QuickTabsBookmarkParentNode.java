package jadx.gui.ui.tab;

import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import jadx.gui.ui.MainWindow;
import jadx.gui.utils.Icons;
import jadx.gui.utils.NLS;

public class QuickTabsBookmarkParentNode extends QuickTabsParentNode {
	protected QuickTabsBookmarkParentNode(TabsController tabsController) {
		super(tabsController);
	}

	@Override
	public String getTitle() {
		return NLS.str("tree.bookmarked_tabs");
	}

	@Override
	Icon getIcon() {
		return Icons.BOOKMARK_DARK;
	}

	@Override
	JPopupMenu onTreePopupMenu(MainWindow mainWindow) {
		if (getChildCount() == 0) {
			return null;
		}

		JPopupMenu menu = new JPopupMenu();
		JMenuItem unbookmarkAll = new JMenuItem(NLS.str("tabs.unbookmark_all"));
		unbookmarkAll.addActionListener(e -> getTabsController().unbookmarkAllTabs());
		menu.add(unbookmarkAll);

		return menu;
	}
}
