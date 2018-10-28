package jadx.gui.ui.codearea;

import javax.swing.*;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
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
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.JumpPosition;

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
			JClass jClsNode = (JClass) this.node;
			((RSyntaxDocument) getDocument()).setSyntaxStyle(new JadxTokenMaker(this, jClsNode));

			setHyperlinksEnabled(true);
			CodeLinkGenerator codeLinkProcessor = new CodeLinkGenerator(contentPanel, this, jClsNode);
			setLinkGenerator(codeLinkProcessor);
			addHyperlinkListener(codeLinkProcessor);
			addMenuItems(jClsNode);
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

	private void addMenuItems(JClass jCls) {
		Action findUsage = new FindUsageAction(contentPanel, this, jCls);

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

	/**
	 * Search node by offset in {@code jCls} code and return its definition position
	 * (useful for jumps from usage)
	 */
	public JumpPosition getDefPosForNodeAtOffset(JClass jCls, int offset) {
		JavaNode foundNode = getJavaNodeAtOffset(jCls, offset);
		if (foundNode == null) {
			return null;
		}
		CodePosition pos = jCls.getCls().getDefinitionPosition(foundNode);
		if (pos == null) {
			return null;
		}
		JNode jNode = contentPanel.getTabbedPane().getMainWindow().getCacheObject().getNodeCache().makeFrom(foundNode);
		return new JumpPosition(jNode.getRootClass(), pos.getLine());
	}

	/**
	 * Search referenced java node by offset in {@code jCls} code
	 */
	public JavaNode getJavaNodeAtOffset(JClass jCls, int offset) {
		try {
			// TODO: add direct mapping for code offset to CodeWriter (instead of line and line offset pair)
			int line = this.getLineOfOffset(offset);
			int lineOffset = offset - this.getLineStartOffset(line);
			return jCls.getCls().getJavaNodeAtPosition(line + 1, lineOffset + 1);
		} catch (Exception e) {
			LOG.error("Can't get java node by offset: {}", offset, e);
		}
		return null;
	}

	public JumpPosition getCurrentPosition() {
		return new JumpPosition(node, getCaretLineNumber() + 1);
	}

	@Nullable
	Integer getSourceLine(int line) {
		return node.getSourceLine(line);
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
}
