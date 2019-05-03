package jadx.gui.ui.codearea;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;
import javax.swing.text.Caret;
import javax.swing.text.DefaultCaret;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.CodePosition;
import jadx.api.JavaNode;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.ContentPanel;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.JumpPosition;

/**
 * The {@link AbstractCodeArea} implementation used for displaying Java code and text based
 * resources (e.g. AndroidManifest.xml)
 */
public final class CodeArea extends AbstractCodeArea {
	private static final Logger LOG = LoggerFactory.getLogger(CodeArea.class);

	private static final long serialVersionUID = 6312736869579635796L;

	CodeArea(ContentPanel contentPanel) {
		super(contentPanel);

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
	}

	@Override
	public void load() {
		if (getText().isEmpty()) {
			setText(node.getContent());
			setCaretPosition(0);
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

	private void addMenuItems(JClass jCls) {
		FindUsageAction findUsage = new FindUsageAction(contentPanel, this, jCls);
		GoToDeclarationAction goToDeclaration = new GoToDeclarationAction(contentPanel, this, jCls);

		JPopupMenu popup = getPopupMenu();
		popup.addSeparator();
		popup.add(findUsage);
		popup.add(goToDeclaration);
		popup.addPopupMenuListener(findUsage);
		popup.addPopupMenuListener(goToDeclaration);
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

}
