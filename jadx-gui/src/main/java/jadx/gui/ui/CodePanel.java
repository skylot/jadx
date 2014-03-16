package jadx.gui.ui;

import jadx.gui.treemodel.JClass;
import jadx.gui.utils.Utils;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import org.fife.ui.rtextarea.RTextScrollPane;

class CodePanel extends JPanel {

	private final TabbedPane codePanel;
	private final JClass jClass;
	private final SearchBar searchBar;
	private final CodeArea codeArea;
	private final RTextScrollPane scrollPane;

	CodePanel(TabbedPane panel, JClass cls) {
		codePanel = panel;
		jClass = cls;
		codeArea = new CodeArea(this);
		searchBar = new SearchBar(codeArea);

		scrollPane = new RTextScrollPane(codeArea);
		scrollPane.setFoldIndicatorEnabled(true);

		setLayout(new BorderLayout());
		add(searchBar, BorderLayout.NORTH);
		add(scrollPane);

		KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK);
		Utils.addKeyBinding(codeArea, key, "SearchAction", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				searchBar.toggle();
			}
		});
	}

	TabbedPane getCodePanel() {
		return codePanel;
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

	RTextScrollPane getScrollPane() {
		return scrollPane;
	}
}
