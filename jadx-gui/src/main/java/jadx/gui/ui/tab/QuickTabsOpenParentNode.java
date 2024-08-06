package jadx.gui.ui.tab;

import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import jadx.gui.ui.MainWindow;
import jadx.gui.utils.Icons;
import jadx.gui.utils.NLS;

public class QuickTabsOpenParentNode extends QuickTabsParentNode {
	protected QuickTabsOpenParentNode(TabsController tabsController) {
		super(tabsController);
	}

	@Override
	public String getTitle() {
		return NLS.str("tree.open_tabs");
	}

	@Override
	Icon getIcon() {
		return Icons.FOLDER;
	}

	@Override
	JPopupMenu onTreePopupMenu(MainWindow mainWindow) {
		if (getChildCount() == 0) {
			return null;
		}

		JPopupMenu menu = new JPopupMenu();
		JMenuItem closeAll = new JMenuItem(NLS.str("tabs.closeAll"));
		closeAll.addActionListener(e -> getTabsController().closeAllTabs(true));
		menu.add(closeAll);

		return menu;
	}
}
