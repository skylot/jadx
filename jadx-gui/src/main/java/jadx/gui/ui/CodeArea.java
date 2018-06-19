package jadx.gui.ui;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import org.fife.ui.rsyntaxtextarea.LinkGenerator;
import org.fife.ui.rsyntaxtextarea.LinkGeneratorResult;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.CodePosition;
import jadx.api.JavaNode;
import jadx.gui.settings.JadxSettings;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.utils.Position;

public final class CodeArea extends RSyntaxTextArea {
	private static final Logger LOG = LoggerFactory.getLogger(CodeArea.class);

	private static final long serialVersionUID = 6312736869579635796L;

	private final CodePanel contentPanel;
	private final JNode node;

	CodeArea(CodePanel panel) {
		this.contentPanel = panel;
		this.node = panel.getNode();

		setMarkOccurrences(true);
		setEditable(false);
		loadSettings();

		Caret caret = getCaret();
		if (caret instanceof DefaultCaret) {
			((DefaultCaret) caret).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		}
		caret.setVisible(true);

		setSyntaxEditingStyle(node.getSyntaxName());
		if (node instanceof JClass) {
			setHyperlinksEnabled(true);
			CodeLinkGenerator codeLinkProcessor = new CodeLinkGenerator((JClass) node);
			setLinkGenerator(codeLinkProcessor);
			addHyperlinkListener(codeLinkProcessor);
			addMenuItems(this, (JClass) node);
		}
		registerWordHighlighter();
		setText(node.getContent());
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

	private void addMenuItems(CodeArea codeArea, JClass jCls) {
		Action findUsage = new FindUsageAction(codeArea, jCls);

		JPopupMenu popup = getPopupMenu();
		popup.addSeparator();
		popup.add(findUsage);
		popup.addPopupMenuListener((PopupMenuListener) findUsage);
	}

	public void loadSettings() {
		loadCommonSettings(contentPanel.getTabbedPane().getMainWindow(), this);
	}

	public static void loadCommonSettings(MainWindow mainWindow, RSyntaxTextArea area) {
		area.setAntiAliasingEnabled(true);
		mainWindow.getEditorTheme().apply(area);

		JadxSettings settings = mainWindow.getSettings();
		area.setFont(settings.getFont());
	}

	public static RSyntaxTextArea getDefaultArea(MainWindow mainWindow) {
		RSyntaxTextArea area = new RSyntaxTextArea();
		loadCommonSettings(mainWindow, area);
		return area;
	}

	private boolean isJumpToken(Token token) {
		if (token.getType() == TokenTypes.IDENTIFIER) {
			// fast skip
			if (token.length() == 1) {
				char ch = token.getTextArray()[token.getTextOffset()];
				if (ch == '.' || ch == ',' || ch == ';') {
					return false;
				}
			}
			if (node instanceof JClass) {
				Position pos = getDefPosition((JClass) node, this, token.getOffset());
				if (pos != null) {
					// don't underline definition place
					try {
						int defLine = pos.getLine();
						int lineOfOffset = getLineOfOffset(token.getOffset()) + 1;
						if (defLine == lineOfOffset) {
							return false;
						}
					} catch (BadLocationException e) {
						return false;
					}
					return true;
				}
			}
		}
		return false;
	}

//	@Override
//	public Color getForegroundForToken(Token t) {
//		if (isJumpToken(t)) {
//			return getHyperlinkForeground();
//		}
//		return super.getForegroundForToken(t);
//	}

	static Position getDefPosition(JClass jCls, RSyntaxTextArea textArea, int offset) {
		JavaNode node = getJavaNodeAtOffset(jCls, textArea, offset);
		if (node == null) {
			return null;
		}
		CodePosition pos = jCls.getCls().getDefinitionPosition(node);
		if (pos == null) {
			return null;
		}
		return new Position(pos);
	}

	static JavaNode getJavaNodeAtOffset(JClass jCls, RSyntaxTextArea textArea, int offset) {
		try {
			int line = textArea.getLineOfOffset(offset);
			int lineOffset = offset - textArea.getLineStartOffset(line);
			return jCls.getCls().getJavaNodeAtPosition(line + 1, lineOffset + 1);
		} catch (BadLocationException e) {
			LOG.error("Can't get java node by offset", e);
		}
		return null;
	}

	public Position getCurrentPosition() {
		return new Position(node, getCaretLineNumber() + 1);
	}

	Integer getSourceLine(int line) {
		return node.getSourceLine(line);
	}

	void scrollToLine(int line) {
		int lineNum = line - 1;
		if (lineNum < 0) {
			lineNum = 0;
		}
		setCaretAtLine(lineNum);
		centerCurrentLine();
		forceCurrentLineHighlightRepaint();
	}

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

	private void setCaretAtLine(int line) {
		try {
			setCaretPosition(getLineStartOffset(line));
		} catch (BadLocationException e) {
			LOG.debug("Can't scroll to {}", line, e);
		}
	}

	private class FindUsageAction extends AbstractAction implements PopupMenuListener {
		private static final long serialVersionUID = 4692546569977976384L;

		private final transient CodeArea codeArea;
		private final transient JClass jCls;

		private transient JavaNode node;

		public FindUsageAction(CodeArea codeArea, JClass jCls) {
			super("Find Usage");
			this.codeArea = codeArea;
			this.jCls = jCls;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (node == null) {
				return;
			}
			MainWindow mainWindow = contentPanel.getTabbedPane().getMainWindow();
			JNode jNode = mainWindow.getCacheObject().getNodeCache().makeFrom(node);
			UsageDialog usageDialog = new UsageDialog(mainWindow, jNode);
			usageDialog.setVisible(true);
		}

		@Override
		public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
			node = null;
			Point pos = codeArea.getMousePosition();
			if (pos != null) {
				Token token = codeArea.viewToToken(pos);
				if (token != null) {
					node = getJavaNodeAtOffset(jCls, codeArea, token.getOffset());
				}
			}
			setEnabled(node != null);
		}

		@Override
		public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
		}

		@Override
		public void popupMenuCanceled(PopupMenuEvent e) {
		}
	}

	private class CodeLinkGenerator implements LinkGenerator, HyperlinkListener {
		private final JClass jCls;

		public CodeLinkGenerator(JClass cls) {
			this.jCls = cls;
		}

		@Override
		public LinkGeneratorResult isLinkAtOffset(RSyntaxTextArea textArea, int offset) {
			try {
				Token token = textArea.modelToToken(offset);
				if (token == null) {
					return null;
				}
				final int sourceOffset = token.getOffset();
				final Position defPos = getDefPosition(jCls, textArea, sourceOffset);
				if (defPos == null) {
					return null;
				}
				return new LinkGeneratorResult() {
					@Override
					public HyperlinkEvent execute() {
						return new HyperlinkEvent(defPos, HyperlinkEvent.EventType.ACTIVATED, null,
								defPos.getNode().makeLongString());
					}

					@Override
					public int getSourceOffset() {
						return sourceOffset;
					}
				};
			} catch (Exception e) {
				LOG.error("isLinkAtOffset error", e);
				return null;
			}
		}

		@Override
		public void hyperlinkUpdate(HyperlinkEvent e) {
			Object obj = e.getSource();
			if (obj instanceof Position) {
				contentPanel.getTabbedPane().codeJump((Position) obj);
			}
		}
	}

	public static final class EditorTheme {
		private final String name;
		private final String path;

		public EditorTheme(String name, String path) {
			this.name = name;
			this.path = path;
		}

		public String getName() {
			return name;
		}

		public String getPath() {
			return path;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	public static EditorTheme[] getAllThemes() {
		return new EditorTheme[]{
				new EditorTheme("default", "/org/fife/ui/rsyntaxtextarea/themes/default.xml"),
				new EditorTheme("eclipse", "/org/fife/ui/rsyntaxtextarea/themes/eclipse.xml"),
				new EditorTheme("idea", "/org/fife/ui/rsyntaxtextarea/themes/idea.xml"),
				new EditorTheme("vs", "/org/fife/ui/rsyntaxtextarea/themes/vs.xml"),
				new EditorTheme("dark", "/org/fife/ui/rsyntaxtextarea/themes/dark.xml"),
				new EditorTheme("monokai", "/org/fife/ui/rsyntaxtextarea/themes/monokai.xml")
		};
	}
}
