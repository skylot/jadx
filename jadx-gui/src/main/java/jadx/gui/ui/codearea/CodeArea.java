package jadx.gui.ui.codearea;

import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

import javax.swing.JPopupMenu;
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
import jadx.gui.jobs.IBackgroundTask;
import jadx.gui.jobs.LoadTask;
import jadx.gui.jobs.TaskWithExtraOnFinish;
import jadx.gui.settings.JadxProject;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JLoadableNode;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JResource;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.action.*;
import jadx.gui.ui.codearea.mode.JCodeMode;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.utils.CaretPositionFix;
import jadx.gui.utils.DefaultPopupMenuListener;
import jadx.gui.utils.JNodeCache;
import jadx.gui.utils.JumpPosition;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.shortcut.ShortcutsController;

/**
 * The {@link AbstractCodeArea} implementation used for displaying Java code and text based
 * resources (e.g. AndroidManifest.xml)
 */
public final class CodeArea extends AbstractCodeArea {
	private static final Logger LOG = LoggerFactory.getLogger(CodeArea.class);

	private static final long serialVersionUID = 6312736869579635796L;

	private @Nullable ICodeInfo cachedCodeInfo;
	private @Nullable MouseHoverHighlighter mouseHoverHighlighter;
	private final ShortcutsController shortcutsController;

	CodeArea(ContentPanel contentPanel, JNode node) {
		super(contentPanel, node);
		this.shortcutsController = getMainWindow().getShortcutsController();

		setSyntaxEditingStyle(node.getSyntaxName());
		boolean isJavaCode = isCodeNode();
		if (isJavaCode) {
			((RSyntaxDocument) getDocument()).setSyntaxStyle(new JadxTokenMaker(this));
		}

		if (node instanceof JResource && node.makeString().endsWith(".json")) {
			addMenuForJsonFile();
		}

		setHyperlinksEnabled(true);
		setCodeFoldingEnabled(true);
		setLinkScanningMask(InputEvent.CTRL_DOWN_MASK);
		CodeLinkGenerator codeLinkGenerator = new CodeLinkGenerator(this);
		setLinkGenerator(codeLinkGenerator);
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.isControlDown() || jumpOnDoubleClick(e)) {
					navToDecl(e.getPoint());
				}
			}
		});

		if (isJavaCode) {
			mouseHoverHighlighter = new MouseHoverHighlighter(this, codeLinkGenerator);
			addMouseMotionListener(mouseHoverHighlighter);
		}
	}

	@Override
	public void loadSettings() {
		super.loadSettings();
		if (mouseHoverHighlighter != null) {
			mouseHoverHighlighter.loadSettings();
		}
	}

	public boolean isCodeNode() {
		return node instanceof JClass || node instanceof JCodeMode;
	}

	private boolean jumpOnDoubleClick(MouseEvent e) {
		return e.getClickCount() == 2 && getMainWindow().getSettings().isJumpOnDoubleClick();
	}

	private void navToDecl(Point point) {
		int offs = viewToModel2D(point);
		JNode node = getJNodeAtOffset(adjustOffsetForWordToken(offs));
		if (node != null) {
			contentPanel.getTabsController().codeJump(node);
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
	public IBackgroundTask getLoadTask() {
		if (node instanceof JLoadableNode) {
			IBackgroundTask loadTask = ((JLoadableNode) node).getLoadTask();
			if (loadTask != null) {
				return new TaskWithExtraOnFinish(loadTask, () -> {
					setText(getCodeInfo().getCodeStr());
					setCaretPosition(0);
					setLoaded();
				});
			}
		}
		return new LoadTask<>(
				() -> getCodeInfo().getCodeStr(),
				code -> {
					setText(code);
					setCaretPosition(0);
					setLoaded();
				});
	}

	@Override
	public void refresh() {
		cachedCodeInfo = null;
		setText(getCodeInfo().getCodeStr());
	}

	@Override
	protected JPopupMenu createPopupMenu() {
		JPopupMenu popup = super.createPopupMenu();
		if (node instanceof JClass) {
			appendCodeMenuItems(popup);
		}
		return popup;
	}

	private void appendCodeMenuItems(JPopupMenu popupMenu) {
		ShortcutsController shortcutsController = getMainWindow().getShortcutsController();
		JNodePopupBuilder popup = new JNodePopupBuilder(this, popupMenu, shortcutsController);
		popup.addSeparator();
		popup.add(new FindUsageAction(this));
		popup.add(new UsageDialogPlusAction(this));
		popup.add(new GoToDeclarationAction(this));
		popup.add(new CommentAction(this));
		popup.add(new CommentSearchAction(this));
		popup.add(new RenameAction(this));
		popup.addSeparator();
		popup.add(new FridaAction(this));
		popup.add(new XposedAction(this));
		getMainWindow().getWrapper().getGuiPluginsContext().appendPopupMenus(this, popup);

		// move caret on mouse right button click
		popupMenu.addPopupMenuListener(new DefaultPopupMenuListener() {
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

	private void addMenuForJsonFile() {
		ShortcutsController shortcutsController = getMainWindow().getShortcutsController();
		JNodePopupBuilder popup = new JNodePopupBuilder(this, getPopupMenu(), shortcutsController);
		popup.addSeparator();
		popup.add(new JsonPrettifyAction(this));
	}

	/**
	 * Search start of word token at specified offset
	 *
	 * @return -1 if no word token found
	 */
	public int adjustOffsetForWordToken(int offset) {
		Token token = getWordTokenAtOffset(offset);
		if (token == null) {
			return -1;
		}
		int type = token.getType();
		if (isCodeNode()) {
			if (type == TokenTypes.IDENTIFIER || type == TokenTypes.FUNCTION) {
				return token.getOffset();
			}
			if (type == TokenTypes.ANNOTATION && token.length() > 1) {
				return token.getOffset() + 1;
			}
			if (type == TokenTypes.RESERVED_WORD && token.length() == 6 && token.getLexeme().equals("static")) {
				// maybe a class init method
				return token.getOffset();
			}
		} else if (type == TokenTypes.MARKUP_TAG_ATTRIBUTE_VALUE) {
			return token.getOffset() + 1; // skip quote at start (")
		}
		return -1;
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
		return getJNodeAtOffset(adjustOffsetForWordToken(caretPos));
	}

	@Nullable
	public JNode getEnclosingNodeUnderCaret() {
		int caretPos = getCaretPosition();
		int start = adjustOffsetForWordToken(caretPos);
		if (start == -1) {
			start = caretPos;
		}
		return getEnclosingJNodeAtOffset(start);
	}

	@Nullable
	public JNode getNodeUnderMouse() {
		Point pos = UiUtils.getMousePosition(this);
		return getJNodeAtOffset(adjustOffsetForWordToken(viewToModel2D(pos)));
	}

	@Nullable
	public JNode getEnclosingNodeUnderMouse() {
		Point pos = UiUtils.getMousePosition(this);
		return getEnclosingJNodeAtOffset(adjustOffsetForWordToken(viewToModel2D(pos)));
	}

	@Nullable
	public JNode getEnclosingJNodeAtOffset(int offset) {
		JavaNode javaNode = getEnclosingJavaNode(offset);
		if (javaNode != null) {
			return convertJavaNode(javaNode);
		}
		return null;
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
		if (offset == -1) {
			return null;
		}
		try {
			return getJadxWrapper().getDecompiler().getClosestJavaNode(getCodeInfo(), offset);
		} catch (Exception e) {
			LOG.error("Can't get java node by offset: {}", offset, e);
			return null;
		}
	}

	public JavaNode getEnclosingJavaNode(int offset) {
		if (offset == -1) {
			return null;
		}
		try {
			return getJadxWrapper().getDecompiler().getEnclosingNode(getCodeInfo(), offset);
		} catch (Exception e) {
			LOG.error("Can't get java node by offset: {}", offset, e);
			return null;
		}
	}

	public @Nullable JavaClass getJavaClassIfAtPos(int pos) {
		try {
			ICodeInfo codeInfo = getCodeInfo();
			if (!codeInfo.hasMetadata()) {
				return null;
			}
			ICodeAnnotation ann = codeInfo.getCodeMetadata().getAt(pos);
			if (ann == null) {
				return null;
			}
			switch (ann.getAnnType()) {
				case CLASS:
					return (JavaClass) getJadxWrapper().getDecompiler().getJavaNodeByCodeAnnotation(codeInfo, ann);
				case METHOD:
					// use class from constructor call
					JavaNode node = getJadxWrapper().getDecompiler().getJavaNodeByCodeAnnotation(codeInfo, ann);
					return node != null ? node.getDeclaringClass() : null;
				default:
					return null;
			}
		} catch (Exception e) {
			LOG.error("Can't get java node by offset: {}", pos, e);
			return null;
		}
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
		return contentPanel.getMainWindow();
	}

	public JadxWrapper getJadxWrapper() {
		return getMainWindow().getWrapper();
	}

	public JadxProject getProject() {
		return getMainWindow().getProject();
	}

	@Override
	public void dispose() {
		shortcutsController.unbindActionsForComponent(this);

		super.dispose();
		cachedCodeInfo = null;
	}
}
