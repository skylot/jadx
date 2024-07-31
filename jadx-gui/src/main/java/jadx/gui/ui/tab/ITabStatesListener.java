package jadx.gui.ui.tab;

import jadx.gui.treemodel.JNode;

public interface ITabStatesListener {
	void onTabPinChange(JNode node, boolean pinned);
}
