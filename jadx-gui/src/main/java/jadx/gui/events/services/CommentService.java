package jadx.gui.events.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import jadx.api.JavaClass;
import jadx.api.data.ICodeComment;
import jadx.api.data.impl.JadxCodeData;
import jadx.gui.jobs.TaskStatus;
import jadx.gui.settings.JadxProject;
import jadx.gui.treemodel.JClass;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.AbstractCodeArea;
import jadx.gui.ui.codearea.AbstractCodeContentPanel;
import jadx.gui.ui.codearea.CodeArea;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.utils.UiUtils;

/**
 * Update code comments in project data and refresh affected code
 */
public class CommentService {

	private final MainWindow mainWindow;

	public CommentService(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
	}

	/**
	 * Apply change to comments list in project code data and notify decompiler.
	 * Should be called on UI thread (same as comments dialog).
	 */
	public static void updateCommentsData(MainWindow mainWindow, Consumer<List<ICodeComment>> updater) {
		JadxProject project = mainWindow.getProject();
		JadxCodeData codeData = project.getCodeData();
		if (codeData == null) {
			codeData = new JadxCodeData();
		}
		List<ICodeComment> list = new ArrayList<>(codeData.getComments());
		updater.accept(list);
		Collections.sort(list);
		codeData.setComments(list);
		project.setCodeData(codeData);
		mainWindow.getWrapper().reloadCodeData();
	}

	/**
	 * Update comments and regenerate code of top-level class in background.
	 * Future completes after code and opened tabs are refreshed.
	 */
	public CompletableFuture<TaskStatus> updateComments(Consumer<List<ICodeComment>> updater, JavaClass topCls) {
		CompletableFuture<TaskStatus> future = new CompletableFuture<>();
		UiUtils.uiRun(() -> {
			JClass jCls;
			try {
				updateCommentsData(mainWindow, updater);
				jCls = mainWindow.getCacheObject().getNodeCache().makeFrom(topCls);
			} catch (Exception e) {
				future.completeExceptionally(e);
				return;
			}
			mainWindow.getBackgroundExecutor().execute("Refreshing",
					() -> jCls.reload(mainWindow.getCacheObject()),
					status -> {
						try {
							refreshTabs(jCls);
						} finally {
							future.complete(status);
						}
					});
		});
		return future;
	}

	private void refreshTabs(JClass cls) {
		for (ContentPanel tab : mainWindow.getTabbedPane().getTabs()) {
			if (tab instanceof AbstractCodeContentPanel && cls.equals(tab.getNode().getRootClass())) {
				AbstractCodeArea codeArea = ((AbstractCodeContentPanel) tab).getCodeArea();
				if (codeArea instanceof CodeArea) {
					((CodeArea) codeArea).refreshClass(true);
				}
			}
		}
	}
}
