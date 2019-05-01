package jadx.gui.ui.codearea;

import jadx.gui.treemodel.JNode;
import jadx.gui.ui.ContentPanel;
import jadx.gui.ui.TabbedPane;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;

/**
 * The basract base class for a content panel that show text based code or a.g. a resource
 */
public abstract class AbstractCodePanel extends ContentPanel {

	protected AbstractCodePanel(TabbedPane panel, JNode jnode) {
		super(panel, jnode);
	}

	public abstract CodeArea getCodeArea();

	protected class SearchAction extends AbstractAction {
		private static final long serialVersionUID = 8650568214755387093L;

		final SearchBar searchBar;

		protected SearchAction(SearchBar searchBar) {
			this.searchBar = searchBar;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			searchBar.toggle();
		}
	}
}
