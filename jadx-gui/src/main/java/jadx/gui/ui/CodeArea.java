package jadx.gui.ui;

import jadx.api.CodePosition;
import jadx.api.JavaNode;
import jadx.gui.settings.JadxSettings;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.utils.Position;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.DefaultCaret;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;

import org.fife.ui.rsyntaxtextarea.LinkGenerator;
import org.fife.ui.rsyntaxtextarea.LinkGeneratorResult;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CodeArea extends RSyntaxTextArea {
	private static final Logger LOG = LoggerFactory.getLogger(CodeArea.class);

	private static final long serialVersionUID = 6312736869579635796L;

	public static final Color BACKGROUND = new Color(0xFAFAFA);
	public static final Color JUMP_TOKEN_FGD = new Color(0x491BA1);

	private final CodePanel contentPanel;
	private final JNode node;

	CodeArea(CodePanel panel) {
		this.contentPanel = panel;
		this.node = panel.getNode();

		setMarkOccurrences(true);
		setBackground(BACKGROUND);
		setAntiAliasingEnabled(true);
		setEditable(false);
		loadSettings();
		Caret caret = getCaret();
		if (caret instanceof DefaultCaret) {
			((DefaultCaret) caret).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		}
		caret.setVisible(true);

		setSyntaxEditingStyle(node.getSyntaxName());

		if (node instanceof JClass) {
			SyntaxScheme scheme = getSyntaxScheme();
			scheme.getStyle(Token.FUNCTION).foreground = Color.BLACK;

			setHyperlinksEnabled(true);
			CodeLinkGenerator codeLinkProcessor = new CodeLinkGenerator((JClass) node);
			setLinkGenerator(codeLinkProcessor);
			addHyperlinkListener(codeLinkProcessor);
			addMenuItems(this, (JClass) node);
		}

		setText(node.getContent());
	}

	private void addMenuItems(CodeArea codeArea, JClass jCls) {
		Action findUsage = new FindUsageAction(codeArea, jCls);
		// TODO: hotkey works only when popup menu is shown
		// findUsage.putValue(Action.ACCELERATOR_KEY, getKeyStroke(KeyEvent.VK_F7, KeyEvent.ALT_DOWN_MASK));

		JPopupMenu popup = getPopupMenu();
		popup.addSeparator();
		popup.add(findUsage);
		popup.addPopupMenuListener((PopupMenuListener) findUsage);
	}

	public void loadSettings() {
		JadxSettings settings = contentPanel.getTabbedPane().getMainWindow().getSettings();
		setFont(settings.getFont());
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
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public Color getForegroundForToken(Token t) {
		if (isJumpToken(t)) {
			return JUMP_TOKEN_FGD;
		}
		return super.getForegroundForToken(t);
	}

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

		private final CodeArea codeArea;
		private final JClass jCls;

		private JavaNode node;

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
}
