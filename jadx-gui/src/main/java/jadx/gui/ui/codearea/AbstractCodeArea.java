package jadx.gui.ui.codearea;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
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
import jadx.gui.treemodel.JEditableNode;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.utils.DefaultPopupMenuListener;
import jadx.gui.utils.JumpPosition;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.ui.DocumentUpdateListener;
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

	protected ContentPanel contentPanel;
	protected JNode node;

	protected volatile boolean loaded = false;

	public AbstractCodeArea(ContentPanel contentPanel, JNode node) {
		this.contentPanel = contentPanel;
		this.node = Objects.requireNonNull(node);

		setMarkOccurrences(false);
		setEditable(node.isEditable());
		setCodeFoldingEnabled(false);
		setFadeCurrentLineHighlight(true);
		setCloseCurlyBraces(true);
		setAntiAliasingEnabled(true);
		loadSettings();

		JadxSettings settings = contentPanel.getTabbedPane().getMainWindow().getSettings();
		setLineWrap(settings.isCodeAreaLineWrap());
		addWrapLineMenuAction(settings);

		ZoomActions.register(this, settings, this::loadSettings);

		if (node instanceof JEditableNode) {
			JEditableNode editableNode = (JEditableNode) node;
			addSaveActions(editableNode);
			addChangeUpdates(editableNode);
		} else {
			addCaretActions();
			addFastCopyAction();
		}
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

	private void addSaveActions(JEditableNode node) {
		addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_S && UiUtils.isCtrlDown(e)) {
					node.save(AbstractCodeArea.this.getText());
					node.setChanged(false);
				}
			}
		});
	}

	private void addChangeUpdates(JEditableNode editableNode) {
		getDocument().addDocumentListener(new DocumentUpdateListener(ev -> {
			if (loaded) {
				editableNode.setChanged(true);
			}
		}));
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
	 * Call `setLoaded()` on load finish.
	 */
	public abstract void load();

	public void setLoaded() {
		this.loaded = true;
	}

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

	public boolean isDisposed() {
		return node == null;
	}

	public void dispose() {
		// code area reference can still be used somewhere in UI objects,
		// reset node reference to allow to GC jadx objects tree
		node = null;
		contentPanel = null;

		// also clear internals
		try {
			setIgnoreRepaint(true);
			setText("");
			setEnabled(false);
			setSyntaxEditingStyle(SYNTAX_STYLE_NONE);
			setLinkGenerator(null);
			for (MouseListener mouseListener : getMouseListeners()) {
				removeMouseListener(mouseListener);
			}
			for (MouseMotionListener mouseMotionListener : getMouseMotionListeners()) {
				removeMouseMotionListener(mouseMotionListener);
			}
			JPopupMenu popupMenu = getPopupMenu();
			for (PopupMenuListener popupMenuListener : popupMenu.getPopupMenuListeners()) {
				popupMenu.removePopupMenuListener(popupMenuListener);
			}
			for (Component component : popupMenu.getComponents()) {
				if (component instanceof JMenuItem) {
					Action action = ((JMenuItem) component).getAction();
					if (action instanceof JNodeAction) {
						((JNodeAction) action).dispose();
					}
				}
			}
			popupMenu.removeAll();
		} catch (Throwable e) {
			LOG.debug("Error on code area dispose", e);
		}
	}

	@Override
	public Dimension getPreferredSize() {
		try {
			return super.getPreferredSize();
		} catch (Exception e) {
			LOG.warn("Failed to calculate preferred size for code area", e);
			// copied from javax.swing.JTextArea.getPreferredSize (super call above)
			// as a fallback for returned null size
			Dimension d = new Dimension(400, 400);
			Insets insets = getInsets();
			if (getColumns() != 0) {
				d.width = Math.max(d.width, getColumns() * getColumnWidth() + insets.left + insets.right);
			}
			if (getRows() != 0) {
				d.height = Math.max(d.height, getRows() * getRowHeight() + insets.top + insets.bottom);
			}
			return d;
		}
	}
}
