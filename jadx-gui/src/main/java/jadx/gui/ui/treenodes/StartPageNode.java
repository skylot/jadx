package jadx.gui.ui.treenodes;

import javax.swing.Icon;

import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.TabbedPane;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.ui.panel.StartPagePanel;
import jadx.gui.utils.Icons;
import jadx.gui.utils.NLS;

public class StartPageNode extends JNode {
	private static final long serialVersionUID = 8983134608645736174L;

	@Override
	public ContentPanel getContentPanel(TabbedPane tabbedPane) {
		return new StartPagePanel(tabbedPane, this);
	}

	@Override
	public String makeString() {
		return NLS.str("start_page.title");
	}

	@Override
	public Icon getIcon() {
		return Icons.START_PAGE;
	}

	@Override
	public JClass getJParent() {
		return null;
	}
}
