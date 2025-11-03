package jadx.gui.ui.treenodes;

import javax.swing.Icon;

import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.ui.panel.UndisplayedStringsPanel;
import jadx.gui.ui.tab.TabbedPane;
import jadx.gui.utils.Icons;
import jadx.gui.utils.NLS;

public class UndisplayedStringsNode extends JNode {
	private static final long serialVersionUID = 2005158949697898302L;

	private final String undisplayedStings;

	public UndisplayedStringsNode(String undisplayedStings) {
		this.undisplayedStings = undisplayedStings;
	}

	@Override
	public boolean hasContent() {
		return true;
	}

	@Override
	public ContentPanel getContentPanel(TabbedPane tabbedPane) {
		return new UndisplayedStringsPanel(tabbedPane, this);
	}

	@Override
	public String makeString() {
		return NLS.str("msg.non_displayable_chars.title");
	}

	@Override
	public Icon getIcon() {
		return Icons.FONT;
	}

	@Override
	public JClass getJParent() {
		return null;
	}

	@Override
	public String makeDescString() {
		return undisplayedStings;
	}

	@Override
	public boolean supportsQuickTabs() {
		return false;
	}
}
