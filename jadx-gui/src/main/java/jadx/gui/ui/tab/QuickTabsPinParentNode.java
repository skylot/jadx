package jadx.gui.ui.tab;

import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import jadx.gui.ui.MainWindow;
import jadx.gui.utils.Icons;
import jadx.gui.utils.NLS;

public class QuickTabsPinParentNode extends QuickTabsParentNode {
	protected QuickTabsPinParentNode(TabsController tabsController) {
		super(tabsController);
	}

	@Override
	public String getTitle() {
		return NLS.str("tree.pinned_tabs");
	}

	@Override
	Icon getIcon() {
		return Icons.PIN;
	}

	@Override
	JPopupMenu onTreePopupMenu(MainWindow mainWindow) {
		if (getChildCount() == 0) {
			return null;
		}

		JPopupMenu menu = new JPopupMenu();
		JMenuItem unpinAll = new JMenuItem(NLS.str("tabs.unpin_all"));
		unpinAll.addActionListener(e -> getTabsController().unpinAllTabs());
		menu.add(unpinAll);

		return menu;
	}
}
