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

		if (node.isPinnable()) {
			if (menu == null) {
				menu = new JPopupMenu();
			}

			JMenuItem unpinAction = new JMenuItem(NLS.str("tabs.unpin"));
			unpinAction.addActionListener(e -> {
				TabComponent tabComponent = mainWindow.getTabbedPane().getTabComponentByNode(node);
				if (tabComponent != null) {
					tabComponent.togglePin();
				}
			});
			menu.add(unpinAction, 0);
			menu.add(new JPopupMenu.Separator(), 1);
		}

		return menu;
	}

	@Override
	Icon getIcon() {
		return node.getIcon();
	}
}
