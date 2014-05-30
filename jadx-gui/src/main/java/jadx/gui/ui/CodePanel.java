package jadx.gui.ui;

import jadx.gui.treemodel.JClass;
import jadx.gui.utils.Utils;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

class CodePanel extends JPanel {

	private static final long serialVersionUID = 5310536092010045565L;

	private final TabbedPane tabbedPane;
	private final JClass jClass;
	private final SearchBar searchBar;
	private final CodeArea codeArea;
	private final JScrollPane scrollPane;

	CodePanel(TabbedPane panel, JClass cls) {
		tabbedPane = panel;
		jClass = cls;
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

	TabbedPane getTabbedPane() {
		return tabbedPane;
	}

	JClass getCls() {
		return jClass;
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
