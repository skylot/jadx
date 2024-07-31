package jadx.gui.ui.tab;

import javax.swing.Icon;

import jadx.gui.utils.Icons;
import jadx.gui.utils.NLS;

public class QuickTabsPinParentNode extends QuickTabsParentNode {
	protected QuickTabsPinParentNode(TabbedPane tabbedPane) {
		super(tabbedPane);
	}

	@Override
	public String getTitle() {
		return NLS.str("tree.pinned_tabs");
	}

	@Override
	Icon getIcon() {
		return Icons.PIN;
	}
}
