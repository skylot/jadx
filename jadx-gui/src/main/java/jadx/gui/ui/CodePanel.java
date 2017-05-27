package jadx.gui.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import jadx.gui.treemodel.JNode;
import jadx.gui.utils.Utils;

class CodePanel extends ContentPanel {

	private static final long serialVersionUID = 5310536092010045565L;

	private final SearchBar searchBar;
	private final CodeArea codeArea;
	private final JScrollPane scrollPane;

	CodePanel(TabbedPane panel, JNode jnode) {
		super(panel, jnode);

		codeArea = new CodeArea(this);
		searchBar = new SearchBar(codeArea);

		scrollPane = new JScrollPane(codeArea);
		scrollPane.setRowHeaderView(new LineNumbers(codeArea));

		setLayout(new BorderLayout());
		add(searchBar, BorderLayout.NORTH);
		add(scrollPane);

		KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK);
		Utils.addKeyBinding(codeArea, key, "SearchAction", new SearchAction());
	}

	private class SearchAction extends AbstractAction {
		private static final long serialVersionUID = 8650568214755387093L;

		@Override
		public void actionPerformed(ActionEvent e) {
			searchBar.toggle();
		}
	}

	@Override
	public void loadSettings() {
		codeArea.loadSettings();
	}

	@Override
	TabbedPane getTabbedPane() {
		return tabbedPane;
	}

	@Override
	JNode getNode() {
		return node;
	}

	SearchBar getSearchBar() {
		return searchBar;
	}

	CodeArea getCodeArea() {
		return codeArea;
	}

	JScrollPane getScrollPane() {
		return scrollPane;
	}
}
