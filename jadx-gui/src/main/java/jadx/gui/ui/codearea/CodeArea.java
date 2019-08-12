package jadx.gui.ui.codearea;

import javax.swing.*;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.CodePosition;
import jadx.api.JadxDecompiler;
import jadx.api.JavaNode;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.ContentPanel;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.JNodeCache;
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
		setSyntaxEditingStyle(node.getSyntaxName());

		if (node instanceof JClass) {
			((RSyntaxDocument) getDocument()).setSyntaxStyle(new JadxTokenMaker(this));
			addMenuItems();
		}

		setHyperlinksEnabled(true);
		CodeLinkGenerator codeLinkProcessor = new CodeLinkGenerator(this);
		setLinkGenerator(codeLinkProcessor);
		addHyperlinkListener(codeLinkProcessor);
	}

	@Override
	public void load() {
		if (getText().isEmpty()) {
			setText(node.getContent());
			setCaretPosition(0);
		}
	}

	private void addMenuItems() {
		FindUsageAction findUsage = new FindUsageAction(this);
		GoToDeclarationAction goToDeclaration = new GoToDeclarationAction(this);

		JPopupMenu popup = getPopupMenu();
		popup.addSeparator();
		popup.add(findUsage);
		popup.add(goToDeclaration);
		popup.addPopupMenuListener(findUsage);
		popup.addPopupMenuListener(goToDeclaration);
	}

	public int adjustOffsetForToken(@Nullable Token token) {
		if (token == null) {
			return -1;
		}
		int type = token.getType();
		final int sourceOffset;
		if (node instanceof JClass) {
			if (type == TokenTypes.IDENTIFIER) {
				sourceOffset = token.getOffset();
			} else if (type == TokenTypes.ANNOTATION && token.length() > 1) {
				sourceOffset = token.getOffset() + 1;
			} else {
				return -1;
			}
		} else {
			if (type == TokenTypes.MARKUP_TAG_ATTRIBUTE_VALUE) {
				sourceOffset = token.getOffset() + 1; // skip quote at start (")
			} else {
				return -1;
			}
		}
		// fast skip
		if (token.length() == 1) {
			char ch = token.getTextArray()[token.getTextOffset()];
			if (ch == '.' || ch == ',' || ch == ';') {
				return -1;
			}
		}
		return sourceOffset;
	}

	/**
	 * Search node by offset in {@code jCls} code and return its definition position
	 * (useful for jumps from usage)
	 */
	@Nullable
	public JumpPosition getDefPosForNodeAtOffset(int offset) {
		if (offset == -1) {
			return null;
		}
		JavaNode foundNode = getJavaNodeAtOffset(offset);
		if (foundNode == null) {
			return null;
		}
		CodePosition pos = getDecompiler().getDefinitionPosition(foundNode);
		if (pos == null) {
			return null;
		}
		JNode jNode = convertJavaNode(foundNode);
		return new JumpPosition(jNode.getRootClass(), pos.getLine());
	}

	private JNode convertJavaNode(JavaNode javaNode) {
		JNodeCache nodeCache = getMainWindow().getCacheObject().getNodeCache();
		return nodeCache.makeFrom(javaNode);
	}

	@Nullable
	public JNode getJNodeAtOffset(int offset) {
		JavaNode javaNode = getJavaNodeAtOffset(offset);
		if (javaNode != null) {
			return convertJavaNode(javaNode);
		}
		return null;
	}

	/**
	 * Search referenced java node by offset in {@code jCls} code
	 */
	public JavaNode getJavaNodeAtOffset(int offset) {
		if (offset == -1) {
			return null;
		}
		try {
			// TODO: add direct mapping for code offset to CodeWriter (instead of line and line offset pair)
			int line = this.getLineOfOffset(offset);
			int lineOffset = offset - this.getLineStartOffset(line);
			return node.getJavaNodeAtPosition(getDecompiler(), line + 1, lineOffset + 1);
		} catch (Exception e) {
			LOG.error("Can't get java node by offset: {}", offset, e);
		}
		return null;
	}

	public MainWindow getMainWindow() {
		return contentPanel.getTabbedPane().getMainWindow();
	}

	private JadxDecompiler getDecompiler() {
		return getMainWindow().getWrapper().getDecompiler();
	}
}
