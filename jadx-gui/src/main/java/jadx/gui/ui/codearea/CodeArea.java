package jadx.gui.ui.codearea;

import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

import javax.swing.event.PopupMenuEvent;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeInfo;
import jadx.api.JavaClass;
import jadx.api.JavaNode;
import jadx.api.metadata.ICodeAnnotation;
import jadx.gui.JadxWrapper;
import jadx.gui.settings.JadxProject;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.panel.ContentPanel;
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

	private @Nullable ICodeInfo cachedCodeInfo;

	CodeArea(ContentPanel contentPanel, JNode node) {
		super(contentPanel, node);
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
				if (e.isControlDown() || jumpOnDoubleClick(e)) {
					navToDecl(e.getPoint(), codeLinkGenerator);
				}
			}
		});

		if (isJavaCode) {
			addMouseMotionListener(new MouseHoverHighlighter(this, codeLinkGenerator));
		}
	}

	private boolean jumpOnDoubleClick(MouseEvent e) {
		return e.getClickCount() == 2 && getMainWindow().getSettings().isJumpOnDoubleClick();
	}

	@SuppressWarnings("deprecation")
	private void navToDecl(Point point, CodeLinkGenerator codeLinkGenerator) {
		int offs = viewToModel(point);
		JNode node = getJNodeAtOffset(codeLinkGenerator.getLinkSourceOffset(offs));
		if (node != null) {
			contentPanel.getTabbedPane().codeJump(node);
		}
	}

	@Override
	public ICodeInfo getCodeInfo() {
		if (cachedCodeInfo == null) {
			if (isDisposed()) {
				LOG.debug("CodeArea used after dispose!");
				return ICodeInfo.EMPTY;
			}
			cachedCodeInfo = Objects.requireNonNull(node.getCodeInfo());
		}
		return cachedCodeInfo;
	}

	@Override
	public void load() {
		if (getText().isEmpty()) {
			setText(getCodeInfo().getCodeStr());
			setCaretPosition(0);
			setLoaded();
		}
	}

	@Override
	public void refresh() {
		cachedCodeInfo = null;
		setText(getCodeInfo().getCodeStr());
	}

	private void addMenuItems() {
		JNodePopupBuilder popup = new JNodePopupBuilder(this, getPopupMenu());
		popup.addSeparator();
		popup.add(new FindUsageAction(this));
		popup.add(new GoToDeclarationAction(this));
		popup.add(new CommentAction(this));
		popup.add(new CommentSearchAction(this));
		popup.add(new RenameAction(this));
		popup.addSeparator();
		popup.add(new FridaAction(this));
		popup.add(new XposedAction(this));
		getMainWindow().getWrapper().getGuiPluginsContext().appendPopupMenus(this, popup);

		// move caret on mouse right button click
		popup.getMenu().addPopupMenuListener(new DefaultPopupMenuListener() {
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
		if (foundNode == node.getJavaNode()) {
			// current node
			return new JumpPosition(node);
		}
		JNode jNode = convertJavaNode(foundNode);
		return new JumpPosition(jNode);
	}

	private JNode convertJavaNode(JavaNode javaNode) {
		JNodeCache nodeCache = getMainWindow().getCacheObject().getNodeCache();
		return nodeCache.makeFrom(javaNode);
	}

	@Nullable
	public JNode getNodeUnderCaret() {
		int caretPos = getCaretPosition();
		Token token = modelToToken(caretPos);
		if (token == null) {
			return null;
		}
		int start = adjustOffsetForToken(token);
		if (start == -1) {
			start = caretPos;
		}
		return getJNodeAtOffset(start);
	}

	@Nullable
	public JNode getNodeUnderMouse() {
		Point pos = UiUtils.getMousePosition(this);
		int offset = adjustOffsetForToken(viewToToken(pos));
		return getJNodeAtOffset(offset);
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
			return getJadxWrapper().getDecompiler().getJavaNodeAtPosition(getCodeInfo(), offset);
		} catch (Exception e) {
			LOG.error("Can't get java node by offset: {}", offset, e);
		}
		return null;
	}

	public JavaNode getClosestJavaNode(int offset) {
		try {
			return getJadxWrapper().getDecompiler().getClosestJavaNode(getCodeInfo(), offset);
		} catch (Exception e) {
			LOG.error("Can't get java node by offset: {}", offset, e);
			return null;
		}
	}

	public JavaClass getJavaClassIfAtPos(int pos) {
		try {
			ICodeInfo codeInfo = getCodeInfo();
			if (codeInfo.hasMetadata()) {
				ICodeAnnotation ann = codeInfo.getCodeMetadata().getAt(pos);
				if (ann != null && ann.getAnnType() == ICodeAnnotation.AnnType.CLASS) {
					return (JavaClass) getJadxWrapper().getDecompiler().getJavaNodeByCodeAnnotation(codeInfo, ann);
				}
			}
		} catch (Exception e) {
			LOG.error("Can't get java node by offset: {}", pos, e);
		}
		return null;
	}

	public void refreshClass() {
		if (node instanceof JClass) {
			JClass cls = node.getRootClass();
			try {
				CaretPositionFix caretFix = new CaretPositionFix(this);
				caretFix.save();

				cachedCodeInfo = cls.reload(getMainWindow().getCacheObject());

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

	public JadxWrapper getJadxWrapper() {
		return getMainWindow().getWrapper();
	}

	public JadxProject getProject() {
		return getMainWindow().getProject();
	}

	@Override
	public void dispose() {
		super.dispose();
		cachedCodeInfo = null;
	}
}
