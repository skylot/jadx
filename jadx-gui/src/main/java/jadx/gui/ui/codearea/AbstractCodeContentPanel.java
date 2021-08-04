package jadx.gui.ui.codearea;

import jadx.gui.treemodel.JNode;
import jadx.gui.ui.TabbedPane;
import jadx.gui.ui.panel.ContentPanel;

/**
 * The abstract base class for a content panel that show text based code or a.g. a resource
 */
public abstract class AbstractCodeContentPanel extends ContentPanel {
	private static final long serialVersionUID = 4685846894279064422L;

	protected AbstractCodeContentPanel(TabbedPane panel, JNode jnode) {
		super(panel, jnode);
	}

	public abstract AbstractCodeArea getCodeArea();
}
