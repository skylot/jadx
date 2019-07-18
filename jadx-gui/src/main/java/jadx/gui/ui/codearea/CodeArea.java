package jadx.gui.ui.codearea;

import javax.swing.*;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.CodePosition;
import jadx.api.JadxDecompiler;
import jadx.api.JavaNode;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.ContentPanel;
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
			JClass jClsNode = (JClass) this.node;
			((RSyntaxDocument) getDocument()).setSyntaxStyle(new JadxTokenMaker(this));
			addMenuItems(jClsNode);
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

	private void addMenuItems(JClass jCls) {
		FindUsageAction findUsage = new FindUsageAction(contentPanel, this);
		GoToDeclarationAction goToDeclaration = new GoToDeclarationAction(contentPanel, this, jCls);

		JPopupMenu popup = getPopupMenu();
		popup.addSeparator();
		popup.add(findUsage);
		popup.add(goToDeclaration);
		popup.addPopupMenuListener(findUsage);
		popup.addPopupMenuListener(goToDeclaration);
	}

	/**
	 * Search node by offset in {@code jCls} code and return its definition position
	 * (useful for jumps from usage)
	 */
	public JumpPosition getDefPosForNodeAtOffset(int offset) {
		JavaNode foundNode = getJavaNodeAtOffset(offset);
		if (foundNode == null) {
			return null;
		}
		CodePosition pos = getDecompiler().getDefinitionPosition(foundNode);
		if (pos == null) {
			return null;
		}
		JNode jNode = contentPanel.getTabbedPane().getMainWindow().getCacheObject().getNodeCache().makeFrom(foundNode);
		return new JumpPosition(jNode.getRootClass(), pos.getLine());
	}

	/**
	 * Search referenced java node by offset in {@code jCls} code
	 */
	public JavaNode getJavaNodeAtOffset(int offset) {
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

	private JadxDecompiler getDecompiler() {
		return contentPanel.getTabbedPane().getMainWindow().getWrapper().getDecompiler();
	}
}
