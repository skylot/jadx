package jadx.gui.events.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.JavaNode;
import jadx.api.data.ICodeRename;
import jadx.api.data.impl.JadxCodeData;
import jadx.api.plugins.events.JadxEvents;
import jadx.api.plugins.events.types.NodeRenamedByUser;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.jobs.TaskStatus;
import jadx.gui.settings.JadxProject;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JRenameNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.ClassCodeContentPanel;
import jadx.gui.ui.codearea.CodeArea;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.ui.tab.TabbedPane;
import jadx.gui.utils.CacheObject;
import jadx.gui.utils.JNodeCache;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

/**
 * Rename service listen for user rename events.
 * For each event:
 * - add/update rename entry in project code data
 * - update code and/or invalidate cache for related classes
 * - apply all needed UI updates (tabs, classes tree)
 */
public class RenameService {
	private static final Logger LOG = LoggerFactory.getLogger(RenameService.class);

	public static RenameService init(MainWindow mainWindow) {
		RenameService renameService = new RenameService(mainWindow);
		mainWindow.events().global().addListener(JadxEvents.NODE_RENAMED_BY_USER, renameService::process);
		return renameService;
	}

	/**
	 * Rename processing result: status of code refresh task and refreshed top classes
	 */
	public static final class RenameResult {
		private final TaskStatus status;
		private final List<JavaClass> updatedClasses;

		RenameResult(TaskStatus status, List<JavaClass> updatedClasses) {
			this.status = status;
			this.updatedClasses = updatedClasses;
		}

		public TaskStatus getStatus() {
			return status;
		}

		public List<JavaClass> getUpdatedClasses() {
			return updatedClasses;
		}
	}

	private final MainWindow mainWindow;
	private final Map<NodeRenamedByUser, CompletableFuture<RenameResult>> pending =
			Collections.synchronizedMap(new IdentityHashMap<>());

	private RenameService(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
	}

	/**
	 * Send rename event (processed as user rename, plugins listeners notified)
	 * and track completion of project data update and code refresh.
	 * Errors are reported through returned future instead of error dialog.
	 */
	public CompletableFuture<RenameResult> sendRename(NodeRenamedByUser event) {
		CompletableFuture<RenameResult> future = new CompletableFuture<>();
		pending.put(event, future);
		mainWindow.events().send(event);
		return future;
	}

	private void process(NodeRenamedByUser event) {
		CompletableFuture<RenameResult> future = pending.remove(event);
		try {
			LOG.debug("Applying rename event: {}", event);
			long timeStarted = System.nanoTime();
			JRenameNode node = getRenameNode(event);
			updateCodeRenames(set -> processRename(node, event, set));
			refreshState(node, timeStarted, future);
		} catch (Exception e) {
			LOG.error("Rename failed", e);
			if (future != null) {
				future.completeExceptionally(e);
			} else {
				UiUtils.errorMessage(mainWindow, "Rename failed:\n" + Utils.getStackTrace(e));
			}
		}
	}

	private @NotNull JRenameNode getRenameNode(NodeRenamedByUser event) {
		Object renameNode = event.getRenameNode();
		if (renameNode instanceof JRenameNode) {
			return (JRenameNode) renameNode;
		}
		JadxDecompiler decompiler = mainWindow.getWrapper().getDecompiler();
		JavaNode javaNode = decompiler.getJavaNodeByRef(event.getNode());
		if (javaNode != null) {
			JNode node = mainWindow.getCacheObject().getNodeCache().makeFrom(javaNode);
			if (node instanceof JRenameNode) {
				return (JRenameNode) node;
			}
		}
		throw new JadxRuntimeException("Failed to resolve node: " + event.getNode());
	}

	private void processRename(JRenameNode node, NodeRenamedByUser event, Set<ICodeRename> renames) {
		ICodeRename rename = node.buildCodeRename(event.getNewName(), renames);
		renames.remove(rename);
		if (event.isResetName() || event.getNewName().isEmpty()) {
			node.removeAlias();
		} else {
			renames.add(rename);
		}
	}

	private void updateCodeRenames(Consumer<Set<ICodeRename>> updater) {
		JadxProject project = mainWindow.getProject();
		JadxCodeData codeData = project.getCodeData();
		if (codeData == null) {
			codeData = new JadxCodeData();
		}
		Set<ICodeRename> set = new HashSet<>(codeData.getRenames());
		updater.accept(set);
		List<ICodeRename> list = new ArrayList<>(set);
		Collections.sort(list);
		codeData.setRenames(list);
		project.setCodeData(codeData);
	}

	private void refreshState(JRenameNode node, long timeStarted, @Nullable CompletableFuture<RenameResult> future) {
		List<JavaNode> toUpdate = new ArrayList<>();
		node.addUpdateNodes(toUpdate);

		JNodeCache nodeCache = mainWindow.getCacheObject().getNodeCache();
		Set<JClass> updatedTopClasses = toUpdate
				.stream()
				.map(JavaNode::getTopParentClass)
				.map(nodeCache::makeFrom)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		LOG.debug("Classes to update: {}", updatedTopClasses);
		List<JavaClass> updatedClasses = updatedTopClasses.stream()
				.map(JClass::getCls)
				.collect(Collectors.toList());
		if (updatedTopClasses.isEmpty()) {
			if (future != null) {
				future.complete(new RenameResult(TaskStatus.COMPLETE, updatedClasses));
			}
			return;
		}
		mainWindow.getBackgroundExecutor().execute("Refreshing",
				() -> {
					mainWindow.getWrapper().reloadCodeData();
					// Reload all the classes in the background process, rather than using the UI thread for
					// decompilation. We don't just use codeArea.backgroundRefreshClass because it would spawn a
					// separate background process, whereas we would like it to happen in this one.
					for (ContentPanel tab : mainWindow.getTabbedPane().getTabs()) {
						JClass rootClass = tab.getNode().getRootClass();
						if (updatedTopClasses.contains(rootClass)) {
							rootClass.reload(mainWindow.getCacheObject());
						}
					}
					UiUtils.uiRunAndWait(() -> refreshTabs(mainWindow.getTabbedPane(), updatedTopClasses));
					refreshClasses(updatedTopClasses);
					LOG.debug("Finished rename, took " + (System.nanoTime() - timeStarted) + " ns");
				},
				(status) -> {
					try {
						if (status == TaskStatus.CANCEL_BY_MEMORY) {
							mainWindow.showHeapUsageBar();
							UiUtils.errorMessage(mainWindow, NLS.str("message.memoryLow"));
						}
						node.reload(mainWindow);
					} finally {
						if (future != null) {
							future.complete(new RenameResult(status, updatedClasses));
						}
					}
				});
	}

	private void refreshClasses(Set<JClass> updatedTopClasses) {
		CacheObject cache = mainWindow.getCacheObject();
		if (updatedTopClasses.size() < 10) {
			// small batch => reload
			LOG.debug("Classes to reload: {}", updatedTopClasses.size());
			for (JClass cls : updatedTopClasses) {
				try {
					cls.reload(cache);
				} catch (Exception e) {
					LOG.error("Failed to reload class: {}", cls.getFullName(), e);
				}
			}
		} else {
			// big batch => unload
			LOG.debug("Classes to unload: {}", updatedTopClasses.size());
			for (JClass cls : updatedTopClasses) {
				try {
					cls.unload(cache);
				} catch (Exception e) {
					LOG.error("Failed to unload class: {}", cls.getFullName(), e);
				}
			}
		}
	}

	private void refreshTabs(TabbedPane tabbedPane, Set<JClass> updatedClasses) {
		for (ContentPanel tab : tabbedPane.getTabs()) {
			JClass rootClass = tab.getNode().getRootClass();
			if (updatedClasses.remove(rootClass)) {
				ClassCodeContentPanel contentPanel = (ClassCodeContentPanel) tab;
				CodeArea codeArea = (CodeArea) contentPanel.getJavaCodePanel().getCodeArea();
				codeArea.refreshClass(true);
			}
		}
	}
}
