package jadx.gui.ui.codearea;

import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JResource;
import jadx.gui.ui.ContentPanel;
import jadx.gui.ui.TabbedPane;
import jadx.gui.utils.NLS;
import jadx.gui.utils.Utils;

import javax.swing.AbstractAction;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public final class ClassCodePanel extends AbstractCodePanel {
	private static final long serialVersionUID = -7229931102504634591L;

	private final SearchBar searchBar;
	private final CodeArea codeArea;
	private final SmaliArea smaliArea;
	private final JScrollPane codeScrollPane;
	private final JScrollPane smaliScrollPane;
	private JTabbedPane areaTabbedPane = new JTabbedPane(JTabbedPane.BOTTOM);

	public ClassCodePanel(TabbedPane panel, JNode jnode) {
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
		SearchAction searchAction = new SearchAction(searchBar);
		Utils.addKeyBinding(codeArea, key, "SearchAction", searchAction);
		Utils.addKeyBinding(smaliArea, key, "SearchAction", searchAction);

		areaTabbedPane.addChangeListener(e -> {
			if (areaTabbedPane.getSelectedComponent() == smaliScrollPane) {
				smaliArea.load();
				searchBar.setRTextArea(smaliArea);
			} else if (areaTabbedPane.getSelectedComponent() == codeScrollPane) {
				searchBar.setRTextArea(codeArea);
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

	@Override
	public CodeArea getCodeArea() {
		return codeArea;
	}

}
