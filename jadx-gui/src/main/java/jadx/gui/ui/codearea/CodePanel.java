package jadx.gui.ui.codearea;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.*;

import org.fife.ui.rtextarea.RTextScrollPane;

import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JResource;
import jadx.gui.ui.ContentPanel;
import jadx.gui.utils.UiUtils;

/**
 * A panel combining a {@link SearchBar and a scollable {@link CodeArea}
 */
public class CodePanel extends JPanel {
	private static final long serialVersionUID = 1117721869391885865L;

	private final SearchBar searchBar;
	private final AbstractCodeArea codeArea;
	private final RTextScrollPane codeScrollPane;

	public CodePanel(ContentPanel contentPanel, AbstractCodeArea codeArea) {
		this.codeArea = codeArea;
		searchBar = new SearchBar(codeArea);
		codeScrollPane = new RTextScrollPane(codeArea, false);

		setLayout(new BorderLayout());
		add(searchBar, BorderLayout.NORTH);
		add(codeScrollPane, BorderLayout.CENTER);

		KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_F, UiUtils.ctrlButton());
		UiUtils.addKeyBinding(codeArea, key, "SearchAction", new AbstractAction() {
			private static final long serialVersionUID = 71338030532869694L;

			@Override
			public void actionPerformed(ActionEvent e) {
				searchBar.toggle();
			}
		});
	}

	public void loadSettings() {
		codeArea.loadSettings();
		initLineNumbers();
	}

	public void load() {
		codeArea.load();
		initLineNumbers();
	}

	private void initLineNumbers() {
		LineNumbers numbers = new LineNumbers(codeArea);
		numbers.setUseSourceLines(isUseSourceLines());
		codeScrollPane.setRowHeaderView(numbers);
	}

	private boolean isUseSourceLines() {
		JNode node = codeArea.getNode();
		if (node instanceof JResource) {
			JResource resNode = (JResource) node;
			return !resNode.getLineMapping().isEmpty();
		}
		return false;
	}

	public SearchBar getSearchBar() {
		return searchBar;
	}

	public AbstractCodeArea getCodeArea() {
		return codeArea;
	}

	public JScrollPane getCodeScrollPane() {
		return codeScrollPane;
	}
}
