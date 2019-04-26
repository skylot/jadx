package jadx.gui.ui.codearea;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;

import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JResource;
import jadx.gui.ui.ContentPanel;
import jadx.gui.ui.TabbedPane;
import jadx.gui.utils.NLS;
import jadx.gui.utils.Utils;

public final class CodePanel extends ContentPanel {
	private static final long serialVersionUID = 5310536092010045565L;

	private final SearchBar searchBar;
	private final CodeArea codeArea;
	private final SmaliArea smaliArea;
	private final JScrollPane codeScrollPane;
	private final JScrollPane smaliScrollPane;
	private JTabbedPane areaTabbedPane = new JTabbedPane(JTabbedPane.BOTTOM);

	public CodePanel(TabbedPane panel, JNode jnode) {
		super(panel, jnode);

		codeArea = new CodeArea(this);
		smaliArea = new SmaliArea(this);
		searchBar = new SearchBar(codeArea);
		codeScrollPane = new JScrollPane(codeArea);
		smaliScrollPane = new JScrollPane(smaliArea);
		initLineNumbers();

		setLayout(new BorderLayout());
		add(searchBar, BorderLayout.NORTH);

		areaTabbedPane.add(codeScrollPane, NLS.str("tabs.code"));
		areaTabbedPane.add(smaliScrollPane, NLS.str("tabs.smali"));
		add(areaTabbedPane);

		KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_F, Utils.ctrlButton());
		Utils.addKeyBinding(codeArea, key, "SearchAction", new SearchAction());

		areaTabbedPane.addChangeListener(e -> {
			if (areaTabbedPane.getSelectedComponent() == smaliScrollPane) {
				smaliArea.load();
			}
		});
	}

	private void initLineNumbers() {
		// TODO: fix slow line rendering on big files
		if (codeArea.getDocument().getLength() <= 100_000) {
			LineNumbers numbers = new LineNumbers(codeArea);
			numbers.setUseSourceLines(isUseSourceLines());
			codeScrollPane.setRowHeaderView(numbers);
		}
	}

	private boolean isUseSourceLines() {
		if (node instanceof JClass) {
			return true;
		}
		if (node instanceof JResource) {
			JResource resNode = (JResource) node;
			return !resNode.getLineMapping().isEmpty();
		}
		return false;
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
		initLineNumbers();
		updateUI();
	}

	@Override
	public TabbedPane getTabbedPane() {
		return tabbedPane;
	}

	@Override
	public JNode getNode() {
		return node;
	}

	SearchBar getSearchBar() {
		return searchBar;
	}

	public CodeArea getCodeArea() {
		return codeArea;
	}

}
