package jadx.gui.ui.codearea;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.DefaultCaret;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.utils.StringUtils;
import jadx.gui.settings.JadxSettings;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.ContentPanel;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.DefaultPopupMenuListener;
import jadx.gui.utils.JumpPosition;
import jadx.gui.utils.NLS;

public abstract class AbstractCodeArea extends RSyntaxTextArea {
	private static final long serialVersionUID = -3980354865216031972L;

	private static final Logger LOG = LoggerFactory.getLogger(AbstractCodeArea.class);

	protected final ContentPanel contentPanel;
	protected final JNode node;

	public AbstractCodeArea(ContentPanel contentPanel) {
		this.contentPanel = contentPanel;
		this.node = contentPanel.getNode();

		setMarkOccurrences(false);
		setEditable(false);
		setCodeFoldingEnabled(false);
		loadSettings();
		JadxSettings settings = contentPanel.getTabbedPane().getMainWindow().getSettings();
		setLineWrap(settings.isCodeAreaLineWrap());

		JPopupMenu popupMenu = getPopupMenu();
		popupMenu.addSeparator();
		JCheckBoxMenuItem wrapItem = new JCheckBoxMenuItem(NLS.str("popup.line_wrap"), getLineWrap());
		wrapItem.setAction(new AbstractAction(NLS.str("popup.line_wrap")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean wrap = !getLineWrap();
				settings.setCodeAreaLineWrap(wrap);
				contentPanel.getTabbedPane().getOpenTabs().values().forEach(v -> {
					if (v instanceof AbstractCodeContentPanel) {
						AbstractCodeArea codeArea = ((AbstractCodeContentPanel) v).getCodeArea();
						setCodeAreaLineWrap(codeArea, wrap);
						if (v instanceof ClassCodeContentPanel) {
							codeArea = ((ClassCodeContentPanel) v).getSmaliCodeArea();
							setCodeAreaLineWrap(codeArea, wrap);
						}
					}
				});
				settings.sync();
			}
		});
		popupMenu.add(wrapItem);
		popupMenu.addPopupMenuListener(new DefaultPopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				wrapItem.setState(getLineWrap());
			}
		});

		Caret caret = getCaret();
		if (caret instanceof DefaultCaret) {
			((DefaultCaret) caret).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		}
		this.addFocusListener(new FocusListener() {
			// fix caret missing bug.
			// when lost focus set visible to false,
			// and when regained set back to true will force
			// the caret to be repainted.
			@Override
			public void focusGained(FocusEvent e) {
				caret.setVisible(true);
			}

			@Override
			public void focusLost(FocusEvent e) {
				caret.setVisible(false);
			}
		});
		addCaretListener(new CaretListener() {
			int lastPos = -1;
			String lastText = "";

			@Override
			public void caretUpdate(CaretEvent e) {
				int pos = getCaretPosition();
				if (lastPos != pos) {
					lastPos = pos;
					lastText = highlightCaretWord(lastText, pos);
				}
			}
		});
	}

	private void setCodeAreaLineWrap(AbstractCodeArea codeArea, boolean wrap) {
		codeArea.setLineWrap(wrap);
		if (codeArea.isVisible()) {
			codeArea.repaint();
		}
	}

	private String highlightCaretWord(String lastText, int pos) {
		String text = getWordByPosition(pos);
		if (StringUtils.isEmpty(text)) {
			highlightAllMatches(null);
			lastText = "";
		} else if (!lastText.equals(text)) {
			highlightAllMatches(text);
			lastText = text;
		}
		return lastText;
	}

	public String getWordUnderCaret() {
		return getWordByPosition(getCaretPosition());
	}

	public int getWordStart(int pos) {
		int start = Math.max(0, pos - 1);
		try {
			if (!StringUtils.isWordSeparator(getText(start, 1).charAt(0))) {
				do {
					start--;
				} while (start >= 0 && !StringUtils.isWordSeparator(getText(start, 1).charAt(0)));
			}
			start++;
		} catch (BadLocationException e) {
			e.printStackTrace();
			start = -1;
		}
		return start;
	}

	public int getWordEnd(int pos, int max) {
		int end = pos;
		try {
			if (!StringUtils.isWordSeparator(getText(end, 1).charAt(0))) {
				do {
					end++;
				} while (end < max && !StringUtils.isWordSeparator(getText(end, 1).charAt(0)));
			}
		} catch (BadLocationException e) {
			e.printStackTrace();
			end = max;
		}
		return end;
	}

	public String getWordByPosition(int pos) {
		String text;
		int len = getDocument().getLength();
		int start = getWordStart(pos);
		int end = getWordEnd(pos, len);
		try {
			if (end > start) {
				text = getText(start, end - start);
			} else {
				text = null;
			}
		} catch (BadLocationException e) {
			e.printStackTrace();
			System.out.printf("start: %d end: %d%n", start, end);
			text = null;
		}
		return text;
	}

	/**
	 * Implement in this method the code that loads and sets the content to be displayed
	 */
	public abstract void load();

	/**
	 * Implement in this method the code that reloads node from cache and sets the new content to be
	 * displayed
	 */
	public abstract void refresh();

	public static RSyntaxTextArea getDefaultArea(MainWindow mainWindow) {
		RSyntaxTextArea area = new RSyntaxTextArea();
		area.setEditable(false);
		area.setCodeFoldingEnabled(false);
		loadCommonSettings(mainWindow, area);
		return area;
	}

	public static void loadCommonSettings(MainWindow mainWindow, RSyntaxTextArea area) {
		area.setAntiAliasingEnabled(true);
		mainWindow.getEditorTheme().apply(area);

		JadxSettings settings = mainWindow.getSettings();
		area.setFont(settings.getFont());
	}

	public void loadSettings() {
		loadCommonSettings(contentPanel.getTabbedPane().getMainWindow(), this);
	}

	public void scrollToPos(int pos) {
		try {
			setCaretPosition(pos);
		} catch (Exception e) {
			LOG.debug("Can't scroll to position {}", pos, e);
		}
		centerCurrentLine();
		forceCurrentLineHighlightRepaint();
	}

	public void scrollToLine(int line) {
		int lineNum = line - 1;
		if (lineNum < 0) {
			lineNum = 0;
		}
		setCaretAtLine(lineNum);
		centerCurrentLine();
		forceCurrentLineHighlightRepaint();
	}

	private void setCaretAtLine(int line) {
		try {
			setCaretPosition(getLineStartOffset(line));
		} catch (BadLocationException e) {
			LOG.debug("Can't scroll to {}", line, e);
		}
	}

	@SuppressWarnings("deprecation")
	public void centerCurrentLine() {
		JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, this);
		if (viewport == null) {
			return;
		}
		try {
			Rectangle r = modelToView(getCaretPosition());
			if (r == null) {
				return;
			}
			int extentHeight = viewport.getExtentSize().height;
			Dimension viewSize = viewport.getViewSize();
			if (viewSize == null) {
				return;
			}
			int viewHeight = viewSize.height;

			int y = Math.max(0, r.y - extentHeight / 2);
			y = Math.min(y, viewHeight - extentHeight);

			viewport.setViewPosition(new Point(0, y));
		} catch (BadLocationException e) {
			LOG.debug("Can't center current line", e);
		}
	}

	private void registerWordHighlighter() {
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent evt) {
				if (evt.getClickCount() % 2 == 0 && !evt.isConsumed()) {
					evt.consume();
					String str = getSelectedText();
					if (str != null) {
						highlightAllMatches(str);
					}
				} else {
					highlightAllMatches(null);
				}
			}
		});
	}

	/**
	 * @param str - if null -> reset current highlights
	 */
	private void highlightAllMatches(@Nullable String str) {
		SearchContext context = new SearchContext(str);
		context.setMarkAll(true);
		context.setMatchCase(true);
		context.setWholeWord(true);
		SearchEngine.markAll(this, context);
	}

	public JumpPosition getCurrentPosition() {
		return new JumpPosition(node, getCaretLineNumber() + 1, getCaretPosition());
	}

	public String getLineText(int line) throws BadLocationException {
		int lineNum = line - 1;
		int startOffset = getLineStartOffset(lineNum);
		int endOffset = getLineEndOffset(lineNum);
		return getText(startOffset, endOffset - startOffset);
	}

	@Nullable
	Integer getSourceLine(int line) {
		return node.getSourceLine(line);
	}

	public ContentPanel getContentPanel() {
		return contentPanel;
	}

	public JNode getNode() {
		return node;
	}

	@Nullable
	public JClass getJClass() {
		if (node instanceof JClass) {
			return (JClass) node;
		}
		return null;
	}
}
