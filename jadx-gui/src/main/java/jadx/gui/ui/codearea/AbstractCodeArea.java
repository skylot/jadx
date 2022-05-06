package jadx.gui.ui.codearea;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

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

import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeInfo;
import jadx.core.utils.StringUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.settings.JadxSettings;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.utils.DefaultPopupMenuListener;
import jadx.gui.utils.JumpPosition;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.ui.ZoomActions;

public abstract class AbstractCodeArea extends RSyntaxTextArea {
	private static final long serialVersionUID = -3980354865216031972L;

	private static final Logger LOG = LoggerFactory.getLogger(AbstractCodeArea.class);

	public static final String SYNTAX_STYLE_SMALI = "text/smali";

	static {
		TokenMakerFactory tokenMakerFactory = TokenMakerFactory.getDefaultInstance();
		if (tokenMakerFactory instanceof AbstractTokenMakerFactory) {
			AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory) tokenMakerFactory;
			atmf.putMapping(SYNTAX_STYLE_SMALI, "jadx.gui.ui.codearea.SmaliTokenMaker");
		} else {
			throw new JadxRuntimeException("Unexpected TokenMakerFactory instance: " + tokenMakerFactory.getClass());
		}
	}

	protected final ContentPanel contentPanel;
	protected final JNode node;

	public AbstractCodeArea(ContentPanel contentPanel, JNode node) {
		this.contentPanel = contentPanel;
		this.node = node;

		setMarkOccurrences(false);
		setEditable(false);
		setCodeFoldingEnabled(false);
		setFadeCurrentLineHighlight(true);
		setCloseCurlyBraces(true);
		setAntiAliasingEnabled(true);
		loadSettings();

		JadxSettings settings = contentPanel.getTabbedPane().getMainWindow().getSettings();
		setLineWrap(settings.isCodeAreaLineWrap());
		addWrapLineMenuAction(settings);

		addCaretActions();
		addFastCopyAction();

		ZoomActions.register(this, settings, this::loadSettings);
	}

	private void addWrapLineMenuAction(JadxSettings settings) {
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
	}

	private void setCodeAreaLineWrap(AbstractCodeArea codeArea, boolean wrap) {
		codeArea.setLineWrap(wrap);
		if (codeArea.isVisible()) {
			codeArea.repaint();
		}
	}

	private void addCaretActions() {
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

	/**
	 * Ctrl+C will copy highlighted word
	 */
	private void addFastCopyAction() {
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_C && UiUtils.isCtrlDown(e)) {
					if (StringUtils.isEmpty(getSelectedText())) {
						UiUtils.copyToClipboard(getWordUnderCaret());
					}
				}
			}
		});
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

	@Nullable
	public String getWordUnderCaret() {
		return getWordByPosition(getCaretPosition());
	}

	@Nullable
	public String getWordByPosition(int pos) {
		try {
			Token token = modelToToken(pos);
			return getWordFromToken(token);
		} catch (Exception e) {
			LOG.error("Failed to get word at pos: {}", pos, e);
			return null;
		}
	}

	@Nullable
	private static String getWordFromToken(@Nullable Token token) {
		if (token == null) {
			return null;
		}
		switch (token.getType()) {
			case TokenTypes.NULL:
			case TokenTypes.WHITESPACE:
			case TokenTypes.SEPARATOR:
			case TokenTypes.OPERATOR:
				return null;

			case TokenTypes.IDENTIFIER:
				if (token.length() == 1) {
					char ch = token.charAt(0);
					if (ch == ';' || ch == '.') {
						return null;
					}
				}
				return token.getLexeme();

			default:
				return token.getLexeme();
		}
	}

	public abstract @NotNull ICodeInfo getCodeInfo();

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
			centerCurrentLine();
			forceCurrentLineHighlightRepaint();
		} catch (Exception e) {
			LOG.warn("Can't scroll to position {}", pos, e);
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

	/**
	 * @param str
	 *            - if null -> reset current highlights
	 */
	private void highlightAllMatches(@Nullable String str) {
		SearchContext context = new SearchContext(str);
		context.setMarkAll(true);
		context.setMatchCase(true);
		context.setWholeWord(true);
		SearchEngine.markAll(this, context);
	}

	public JumpPosition getCurrentPosition() {
		return new JumpPosition(node, getCaretPosition());
	}

	public int getLineStartFor(int pos) throws BadLocationException {
		return getLineStartOffset(getLineOfOffset(pos));
	}

	public String getLineAt(int pos) throws BadLocationException {
		return getLineText(getLineOfOffset(pos) + 1);
	}

	public String getLineText(int line) throws BadLocationException {
		int lineNum = line - 1;
		int startOffset = getLineStartOffset(lineNum);
		int endOffset = getLineEndOffset(lineNum);
		return getText(startOffset, endOffset - startOffset);
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
