package jadx.gui.ui;

import jadx.gui.treemodel.JNode;
import jadx.gui.utils.Utils;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

class ContentPanel extends JPanel {

	private static final long serialVersionUID = 5310536092010045565L;

	private final TabbedPane tabbedPane;
	private final JNode node;
	private final SearchBar searchBar;
	private final ContentArea contentArea;
	private final JScrollPane scrollPane;

	ContentPanel(TabbedPane panel, JNode node) {
		tabbedPane = panel;
		this.node = node;
		contentArea = new ContentArea(this);
		searchBar = new SearchBar(contentArea);

		scrollPane = new JScrollPane(contentArea);
		scrollPane.setRowHeaderView(new LineNumbers(contentArea));

		setLayout(new BorderLayout());
		add(searchBar, BorderLayout.NORTH);
		add(scrollPane);

		KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK);
		Utils.addKeyBinding(contentArea, key, "SearchAction", new SearchAction());
	}

	private class SearchAction extends AbstractAction {
		private static final long serialVersionUID = 8650568214755387093L;

		@Override
		public void actionPerformed(ActionEvent e) {
			searchBar.toggle();
		}
	}

	TabbedPane getTabbedPane() {
		return tabbedPane;
	}

	JNode getNode() {
		return node;
	}

	SearchBar getSearchBar() {
		return searchBar;
	}

	ContentArea getContentArea() {
		return contentArea;
	}

	JScrollPane getScrollPane() {
		return scrollPane;
	}
}
