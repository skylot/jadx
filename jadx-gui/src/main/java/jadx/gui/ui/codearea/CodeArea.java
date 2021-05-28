package jadx.gui.ui.codearea;

import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.CodePosition;
import jadx.api.JadxDecompiler;
import jadx.api.JavaNode;
import jadx.gui.settings.JadxProject;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.ContentPanel;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.CaretPositionFix;
import jadx.gui.utils.DefaultPopupMenuListener;
import jadx.gui.utils.JNodeCache;
import jadx.gui.utils.JumpPosition;
import jadx.gui.utils.UiUtils;

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

		boolean isJavaCode = node instanceof JClass;
		if (isJavaCode) {
			((RSyntaxDocument) getDocument()).setSyntaxStyle(new JadxTokenMaker(this));
			addMenuItems();
		}

		setHyperlinksEnabled(true);
		setLinkScanningMask(InputEvent.CTRL_DOWN_MASK);
		CodeLinkGenerator codeLinkGenerator = new CodeLinkGenerator(this);
		setLinkGenerator(codeLinkGenerator);
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() % 2 == 0 || e.isControlDown()) {
					navToDecl(e.getPoint(), codeLinkGenerator);
				}
			}
		});

		if (isJavaCode) {
			addMouseMotionListener(new MouseHoverHighlighter(this, codeLinkGenerator));
		}
	}

	@SuppressWarnings("deprecation")
	private void navToDecl(Point point, CodeLinkGenerator codeLinkGenerator) {
		int offs = viewToModel(point);
		JumpPosition jump = codeLinkGenerator.getJumpLinkAtOffset(CodeArea.this, offs);
		if (jump != null) {
			contentPanel.getTabbedPane().codeJump(jump);
		}
	}

	@Override
	public void load() {
		if (getText().isEmpty()) {
			setText(node.getContent());
			setCaretPosition(0);
		}
	}

	@Override
	public void refresh() {
		setText(node.getContent());
	}

	private void addMenuItems() {
		FindUsageAction findUsage = new FindUsageAction(this);
		GoToDeclarationAction goToDeclaration = new GoToDeclarationAction(this);
		RenameAction rename = new RenameAction(this);
		CommentAction comment = new CommentAction(this);

		JPopupMenu popup = getPopupMenu();
		popup.addSeparator();
		popup.add(findUsage);
		popup.add(goToDeclaration);
		popup.add(comment);
		popup.add(new CommentSearchAction(this));
		popup.add(rename);
		popup.addPopupMenuListener(findUsage);
		popup.addPopupMenuListener(goToDeclaration);
		popup.addPopupMenuListener(comment);
		popup.addPopupMenuListener(rename);

		// move caret on mouse right button click
		popup.addPopupMenuListener(new DefaultPopupMenuListener() {
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
				CodeArea codeArea = CodeArea.this;
				if (codeArea.getSelectedText() == null) {
					int offset = UiUtils.getOffsetAtMousePosition(codeArea);
					if (offset >= 0) {
						codeArea.setCaretPosition(offset);
					}
				}
			}
		});
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
		return new JumpPosition(jNode.getRootClass(), pos.getLine(), JumpPosition.getDefPos(jNode));
	}

	private JNode convertJavaNode(JavaNode javaNode) {
		JNodeCache nodeCache = getMainWindow().getCacheObject().getNodeCache();
		return nodeCache.makeFrom(javaNode);
	}

	public JNode getNodeUnderCaret() {
		int start = getWordStart(getCaretPosition());
		if (start == -1) {
			start = getCaretPosition();
		}
		return getJNodeAtOffset(start);
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

	public void refreshClass() {
		if (node instanceof JClass) {
			JClass cls = (JClass) node;
			try {
				CaretPositionFix caretFix = new CaretPositionFix(this);
				caretFix.save();

				cls.reload();
				getMainWindow().getCacheObject().getIndexService().refreshIndex(cls.getCls());

				ClassCodeContentPanel codeContentPanel = (ClassCodeContentPanel) this.contentPanel;
				codeContentPanel.getTabbedPane().refresh(cls);
				codeContentPanel.getJavaCodePanel().refresh(caretFix);
			} catch (Exception e) {
				LOG.error("Failed to reload class: {}", cls.getFullName(), e);
			}
		}
	}

	public MainWindow getMainWindow() {
		return contentPanel.getTabbedPane().getMainWindow();
	}

	public JadxDecompiler getDecompiler() {
		return getMainWindow().getWrapper().getDecompiler();
	}

	public JadxProject getProject() {
		return getMainWindow().getProject();
	}
}
