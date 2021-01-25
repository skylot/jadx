package jadx.gui.ui.codearea;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.*;
import javax.swing.text.BadLocationException;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.utils.StringUtils;
import jadx.gui.utils.NLS;
import jadx.gui.utils.TextStandardActions;
import jadx.gui.utils.UiUtils;

class SearchBar extends JToolBar {
	private static final long serialVersionUID = 1836871286618633003L;

	private static final Logger LOG = LoggerFactory.getLogger(SearchBar.class);

	private static final Color COLOR_BG_ERROR = new Color(0xFFDFDE);
	private static final Color COLOR_BG_WARN = new Color(0xFFFDD9);
	private static final Color COLOR_BG_NORMAL = new Color(0xFFFFFF);

	private static final Icon ICON_UP = UiUtils.openIcon("arrow_up");
	private static final Icon ICON_DOWN = UiUtils.openIcon("arrow_down");
	private static final Icon ICON_CLOSE = UiUtils.openIcon("cross");

	private RSyntaxTextArea rTextArea;

	private final JTextField searchField;
	private final JCheckBox markAllCB;
	private final JCheckBox regexCB;

	private final JCheckBox wholeWordCB;
	private final JCheckBox matchCaseCB;

	public SearchBar(RSyntaxTextArea textArea) {
		rTextArea = textArea;

		JLabel findLabel = new JLabel(NLS.str("search.find") + ':');
		add(findLabel);

		searchField = new JTextField(30);
		searchField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				switch (e.getKeyCode()) {
					case KeyEvent.VK_ENTER:
						// skip
						break;
					case KeyEvent.VK_ESCAPE:
						toggle();
						break;
					default:
						search(0);
						break;
				}
			}
		});
		searchField.addActionListener(e -> search(1));
		new TextStandardActions(searchField);
		add(searchField);

		JButton prevButton = new JButton(NLS.str("search.previous"));
		prevButton.setIcon(ICON_UP);
		prevButton.addActionListener(e -> search(-1));
		prevButton.setBorderPainted(false);
		add(prevButton);

		JButton nextButton = new JButton(NLS.str("search.next"));
		nextButton.setIcon(ICON_DOWN);
		nextButton.addActionListener(e -> search(1));
		nextButton.setBorderPainted(false);
		add(nextButton);

		ActionListener forwardListener = e -> search(0);

		markAllCB = new JCheckBox(NLS.str("search.mark_all"));
		markAllCB.addActionListener(forwardListener);
		add(markAllCB);

		regexCB = new JCheckBox(NLS.str("search.regex"));
		regexCB.addActionListener(forwardListener);
		add(regexCB);

		matchCaseCB = new JCheckBox(NLS.str("search.match_case"));
		matchCaseCB.addActionListener(forwardListener);
		add(matchCaseCB);

		wholeWordCB = new JCheckBox(NLS.str("search.whole_word"));
		wholeWordCB.addActionListener(forwardListener);
		add(wholeWordCB);

		JButton closeButton = new JButton();
		closeButton.setIcon(ICON_CLOSE);
		closeButton.addActionListener(e -> toggle());
		closeButton.setBorderPainted(false);
		add(closeButton);

		setFloatable(false);
		setVisible(false);
	}

	public boolean toggle() {
		boolean visible = !isVisible();
		setVisible(visible);

		if (visible) {
			String preferText = rTextArea.getSelectedText();
			if (!StringUtils.isEmpty(preferText)) {
				searchField.setText(preferText);
			}
			searchField.requestFocus();
			searchField.selectAll();
		} else {
			rTextArea.requestFocus();
		}
		return visible;
	}

	private void search(int direction) {
		String searchText = searchField.getText();
		if (searchText == null
				|| searchText.length() == 0
				|| rTextArea.getText() == null) {
			return;
		}

		boolean forward = direction >= 0;
		boolean matchCase = matchCaseCB.isSelected();
		boolean regex = regexCB.isSelected();
		boolean wholeWord = wholeWordCB.isSelected();

		SearchContext context = new SearchContext();
		context.setSearchFor(searchText);
		context.setMatchCase(matchCase);
		context.setRegularExpression(regex);
		context.setSearchForward(forward);
		context.setWholeWord(wholeWord);
		context.setMarkAll(markAllCB.isSelected());

		// TODO hack: move cursor before previous search for not jump to next occurrence
		if (direction == 0 && !COLOR_BG_ERROR.equals(searchField.getBackground())) {
			try {
				int caretPos = rTextArea.getCaretPosition();
				int lineNum = rTextArea.getLineOfOffset(caretPos) - 1;
				if (lineNum > 1) {
					rTextArea.setCaretPosition(rTextArea.getLineStartOffset(lineNum));
				}
			} catch (BadLocationException e) {
				LOG.error("Caret move error", e);
			}
		}

		SearchResult result = SearchEngine.find(rTextArea, context);
		if (!result.wasFound()) {
			int pos = SearchEngine.getNextMatchPos(searchText, rTextArea.getText(), forward, matchCase, wholeWord);
			if (pos != -1) {
				rTextArea.setCaretPosition(forward ? 0 : rTextArea.getDocument().getLength() - 1);
				search(direction);
				searchField.setBackground(COLOR_BG_WARN);
				return;
			}
			searchField.setBackground(COLOR_BG_ERROR);
		} else {
			searchField.setBackground(COLOR_BG_NORMAL);
		}
	}

	public void setRTextArea(RSyntaxTextArea rTextArea) {
		this.rTextArea = rTextArea;
		if (isVisible()) {
			this.search(0);
		}
	}
}
