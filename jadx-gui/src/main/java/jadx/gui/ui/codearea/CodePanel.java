package jadx.gui.ui.codearea;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.*;
import javax.swing.border.EmptyBorder;

import jadx.api.ICodeInfo;
import jadx.gui.utils.UiUtils;

/**
 * A panel combining a {@link SearchBar and a scollable {@link CodeArea}
 */
public class CodePanel extends JPanel {
	private static final long serialVersionUID = 1117721869391885865L;

	private final SearchBar searchBar;
	private final AbstractCodeArea codeArea;
	private final JScrollPane codeScrollPane;

	public CodePanel(AbstractCodeArea codeArea) {
		this.codeArea = codeArea;
		searchBar = new SearchBar(codeArea);
		codeScrollPane = new JScrollPane(codeArea);

		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(0, 0, 0, 0));
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
		if (codeArea instanceof SmaliArea) {
			return false;
		}
		ICodeInfo codeInfo = codeArea.getNode().getCodeInfo();
		if (codeInfo == null) {
			return false;
		}
		Map<Integer, Integer> lineMapping = codeInfo.getLineMapping();
		if (lineMapping.isEmpty()) {
			return false;
		}
		Set<Integer> uniqueSourceLines = new HashSet<>(lineMapping.values());
		return uniqueSourceLines.size() > 3;
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
