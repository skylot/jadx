package jadx.gui.mcp;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;

import jadx.api.JavaClass;
import jadx.api.JavaNode;
import jadx.api.JavaPackage;
import jadx.api.JavaVariable;
import jadx.api.data.ICodeComment;
import jadx.api.data.impl.JadxCodeData;
import jadx.api.plugins.events.types.NodeRenamedByUser;
import jadx.core.utils.Utils;
import jadx.gui.events.services.CommentService;
import jadx.gui.events.services.RenameService;
import jadx.gui.jobs.TaskStatus;
import jadx.gui.settings.JadxProject;
import jadx.gui.settings.data.SaveOptionEnum;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JMethod;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JRenameNode;
import jadx.gui.treemodel.JResource;
import jadx.gui.treemodel.JVariable;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.AbstractCodeArea;
import jadx.gui.ui.codearea.AbstractCodeContentPanel;
import jadx.gui.ui.codearea.ClassCodeContentPanel;
import jadx.gui.ui.codearea.CodeArea;
import jadx.gui.ui.codearea.SmaliArea;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.utils.JNodeCache;
import jadx.gui.utils.JumpPosition;
import jadx.gui.utils.pkgs.JRenamePackage;
import jadx.mcp.jadx.CodeText;
import jadx.mcp.jadx.NodeIds;
import jadx.mcp.project.CommentsUpdate;
import jadx.mcp.project.McpBackend;
import jadx.mcp.project.ProjectGate;
import jadx.mcp.project.ProjectLease;
import jadx.mcp.project.RenameRequest;
import jadx.mcp.project.SaveStatus;
import jadx.mcp.project.UiState;
import jadx.mcp.project.WriteResult;
import jadx.mcp.tools.ToolCallContext;
import jadx.mcp.tools.ToolErrorCode;
import jadx.mcp.tools.ToolException;

/**
 * MCP backend for jadx-gui.
 * UI state is read on EDT with bounded wait; modifications go through the same services as GUI
 * actions
 * (rename event processing and comments update), and calls wait for code refresh completion.
 */
public class GuiMcpBackend implements McpBackend {
	private static final long UI_CALL_TIMEOUT_MS = 10_000;
	private static final long POLL_MS = 100;

	private final MainWindow mainWindow;
	private final ProjectGate gate = new ProjectGate();
	private final CommentService commentService;

	public GuiMcpBackend(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
		this.commentService = new CommentService(mainWindow);
	}

	@Override
	public ProjectGate getGate() {
		return gate;
	}

	@Override
	public void fillProjectInfo(ProjectLease lease, JsonObject info) {
		JadxProject project = mainWindow.getProject();
		info.addProperty("name", project.getName());
		Path projectPath = project.getProjectPath();
		info.addProperty("project_file", projectPath == null ? null : projectPath.toAbsolutePath().toString());
		info.addProperty("saved", project.isSaved());
	}

	@Override
	public boolean hasUi() {
		return true;
	}

	@Override
	public UiState getUiState(ProjectLease lease, ToolCallContext ctx) throws ToolException {
		return onEdt(ctx, this::snapshotUi);
	}

	private UiState snapshotUi() {
		ContentPanel panel = mainWindow.getTabbedPane().getSelectedContentPanel();
		if (panel == null) {
			return UiState.noTab();
		}
		JNode node = panel.getNode();
		String title = node.makeLongString();
		AbstractCodeArea area = null;
		if (panel instanceof ClassCodeContentPanel) {
			area = ((ClassCodeContentPanel) panel).getCurrentCodeArea();
		} else if (panel instanceof AbstractCodeContentPanel) {
			area = ((AbstractCodeContentPanel) panel).getCodeArea();
		}
		String kind;
		String resourceName = null;
		if (node instanceof JResource) {
			kind = "resource";
			resourceName = ((JResource) node).getName();
		} else if (area instanceof SmaliArea) {
			kind = "smali";
		} else if (area instanceof CodeArea) {
			kind = "code";
		} else {
			kind = "other";
		}
		JClass rootClass = node.getRootClass();
		JavaNode tabNode = rootClass != null ? rootClass.getCls() : null;
		if (area == null) {
			return new UiState(title, kind, tabNode, resourceName, null, -1, -1, -1, -1, -1, -1, null, null);
		}
		String selected = area.getSelectedText();
		int selStart = area.getSelectionStart();
		int selEnd = area.getSelectionEnd();
		int caret = area.getCaretPosition();
		JavaNode underCaret = null;
		JavaNode enclosing = null;
		if (area instanceof CodeArea) {
			CodeArea codeArea = (CodeArea) area;
			underCaret = javaNodeOf(codeArea.getNodeUnderCaret());
			enclosing = javaNodeOf(codeArea.getEnclosingNodeUnderCaret());
		}
		return new UiState(title, kind, tabNode, resourceName, selected, selStart, selEnd,
				caret, lineOf(area, caret), lineOf(area, selStart), lineOf(area, selEnd),
				underCaret, enclosing);
	}

	private static @Nullable JavaNode javaNodeOf(@Nullable JNode node) {
		return node == null ? null : node.getJavaNode();
	}

	private static int lineOf(AbstractCodeArea area, int pos) {
		try {
			return area.getLineOfOffset(pos) + 1;
		} catch (BadLocationException e) {
			return -1;
		}
	}

	@Override
	public void openInUi(ProjectLease lease, JavaNode node, @Nullable Integer line, ToolCallContext ctx) throws ToolException {
		if (node instanceof JavaPackage) {
			throw new ToolException(ToolErrorCode.UNSUPPORTED, "Packages can't be opened in editor, open a class instead");
		}
		Integer pos = null;
		JavaClass topCls = node.getTopParentClass();
		if (line != null && topCls != null) {
			CodeText code = new CodeText(topCls.getCode());
			lease.check();
			if (line > code.getLinesCount()) {
				throw ToolException.invalidArg("Line " + line + " is out of range (" + code.getLinesCount() + " lines)");
			}
			pos = code.lineStart(line);
		}
		Integer jumpPos = pos;
		onEdt(ctx, () -> {
			JNodeCache nodeCache = mainWindow.getCacheObject().getNodeCache();
			if (jumpPos != null) {
				JClass jCls = nodeCache.makeFrom(topCls);
				mainWindow.getTabsController().codeJump(new JumpPosition(jCls, jumpPos));
			} else {
				JNode jNode = nodeCache.makeFrom(node);
				if (jNode == null) {
					throw new ToolException(ToolErrorCode.UNSUPPORTED, "Node can't be opened: " + NodeIds.id(node));
				}
				mainWindow.getTabsController().codeJump(jNode);
			}
			return null;
		});
	}

	@Override
	public WriteResult rename(ProjectLease lease, RenameRequest request, ToolCallContext ctx) throws ToolException {
		RenameService renameService = mainWindow.getRenameService();
		NodeRenamedByUser event = onEdt(ctx, () -> {
			// same steps as in rename dialog
			JRenameNode renameNode = buildRenameNode(request.getNode());
			String oldName = renameNode.getName();
			String newName;
			if (request.isReset()) {
				renameNode.removeAlias();
				newName = Utils.getOrElse(renameNode.getJavaNode().getName(), "");
			} else {
				newName = request.getNewName();
			}
			NodeRenamedByUser ev = new NodeRenamedByUser(renameNode.getJavaNode().getCodeNodeRef(), oldName, newName);
			ev.setRenameNode(renameNode);
			ev.setResetName(request.isReset());
			return ev;
		});
		RenameService.RenameResult result = waitFor(renameService.sendRename(event), ctx, ctx.remainingMs());
		Set<String> updated = new TreeSet<>();
		for (JavaClass cls : result.getUpdatedClasses()) {
			updated.add(NodeIds.classId(cls));
		}
		String warning = result.getStatus() == TaskStatus.COMPLETE
				? null
				: "Rename saved in project data, but code refresh finished with status " + result.getStatus();
		return new WriteResult(new ArrayList<>(updated), getSaveStatus(), warning);
	}

	private JRenameNode buildRenameNode(JavaNode node) throws ToolException {
		JNodeCache nodeCache = mainWindow.getCacheObject().getNodeCache();
		if (node instanceof JavaPackage) {
			JavaPackage pkg = (JavaPackage) node;
			return new JRenamePackage(pkg, pkg.getRawFullName(), pkg.getFullName(), pkg.getName());
		}
		if (node instanceof JavaVariable) {
			JavaVariable var = (JavaVariable) node;
			JNode mthNode = nodeCache.makeFrom(var.getMth());
			if (mthNode instanceof JMethod) {
				return new JVariable((JMethod) mthNode, var);
			}
			throw new ToolException(ToolErrorCode.OPERATION_FAILED, "Failed to resolve method of variable");
		}
		JNode jNode = nodeCache.makeFrom(node);
		if (jNode instanceof JRenameNode) {
			JRenameNode renameNode = (JRenameNode) jNode;
			if (!renameNode.canRename()) {
				throw new ToolException(ToolErrorCode.UNSUPPORTED, "Node can't be renamed: " + NodeIds.id(node));
			}
			return renameNode;
		}
		throw new ToolException(ToolErrorCode.UNSUPPORTED, "Node can't be renamed: " + NodeIds.id(node));
	}

	@Override
	public WriteResult updateComments(ProjectLease lease, CommentsUpdate update, ToolCallContext ctx) throws ToolException {
		CompletableFuture<TaskStatus> future = commentService.updateComments(update.getUpdater(), update.getTopClass());
		TaskStatus status = waitFor(future, ctx, ctx.remainingMs());
		List<String> updated = new ArrayList<>();
		updated.add(NodeIds.classId(update.getTopClass()));
		String warning = status == TaskStatus.COMPLETE
				? null
				: "Comment saved in project data, but code refresh finished with status " + status;
		return new WriteResult(updated, getSaveStatus(), warning);
	}

	@Override
	public List<ICodeComment> getComments(ProjectLease lease) {
		JadxCodeData codeData = mainWindow.getProject().getCodeData();
		if (codeData == null) {
			return new ArrayList<>();
		}
		return new ArrayList<>(codeData.getComments());
	}

	@Override
	public SaveStatus getSaveStatus() {
		JadxProject project = mainWindow.getProject();
		Path projectPath = project.getProjectPath();
		SaveOptionEnum saveOption = mainWindow.getSettings().getSaveOption();
		String autoSave = saveOption.name().toLowerCase(Locale.ROOT);
		if (projectPath == null) {
			return new SaveStatus(false, null, autoSave,
					"Project has no file yet: changes are kept in memory. "
							+ "Save project in jadx-gui (File > Save project as) to persist renames and comments.");
		}
		boolean saved = project.isSaved();
		return new SaveStatus(saved, projectPath.toAbsolutePath().toString(), autoSave,
				saved ? null : "Project has unsaved changes, call 'save_project' or save in jadx-gui");
	}

	@Override
	public SaveStatus saveProject(ProjectLease lease, ToolCallContext ctx) throws ToolException {
		return onEdt(ctx, () -> {
			JadxProject project = mainWindow.getProject();
			if (project.getProjectPath() != null) {
				try {
					project.save();
				} catch (Exception e) {
					throw new ToolException(ToolErrorCode.OPERATION_FAILED, "Project save failed: " + e.getMessage(), e);
				}
				mainWindow.update();
			}
			return getSaveStatus();
		});
	}

	/**
	 * Run on UI thread and wait with timeout and cancellation checks
	 */
	private static <T> T onEdt(ToolCallContext ctx, Callable<T> callable) throws ToolException {
		if (SwingUtilities.isEventDispatchThread()) {
			try {
				return callable.call();
			} catch (ToolException e) {
				throw e;
			} catch (Exception e) {
				throw new ToolException(ToolErrorCode.OPERATION_FAILED, String.valueOf(e.getMessage()), e);
			}
		}
		CompletableFuture<T> future = new CompletableFuture<>();
		SwingUtilities.invokeLater(() -> {
			if (ctx.getToken().isCancelled()) {
				future.cancel(false);
				return;
			}
			try {
				future.complete(callable.call());
			} catch (Throwable e) {
				future.completeExceptionally(e);
			}
		});
		return waitFor(future, ctx, UI_CALL_TIMEOUT_MS);
	}

	private static <T> T waitFor(CompletableFuture<T> future, ToolCallContext ctx, long maxWaitMs) throws ToolException {
		long waitMs = Math.min(Math.min(maxWaitMs, ctx.remainingMs()), TimeUnit.HOURS.toMillis(1));
		long deadline = System.currentTimeMillis() + waitMs;
		while (true) {
			ctx.checkCancelled();
			try {
				return future.get(POLL_MS, TimeUnit.MILLISECONDS);
			} catch (TimeoutException e) {
				if (System.currentTimeMillis() > deadline) {
					throw new ToolException(ToolErrorCode.TIMEOUT, "jadx-gui did not complete operation in time (UI busy?)");
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new ToolException(ToolErrorCode.CANCELLED, "Interrupted");
			} catch (ExecutionException e) {
				Throwable cause = e.getCause();
				if (cause instanceof ToolException) {
					throw (ToolException) cause;
				}
				throw new ToolException(ToolErrorCode.OPERATION_FAILED, String.valueOf(cause), cause);
			} catch (CancellationException e) {
				ctx.checkCancelled();
				throw new ToolException(ToolErrorCode.CANCELLED, "Operation cancelled");
			}
		}
	}
}
