package jadx.gui.jobs;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.utils.Utils;
import jadx.gui.ui.panel.ProgressPanel;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

@SuppressWarnings({ "FieldCanBeLocal", "InfiniteLoopStatement" })
public class ProgressUpdater {
	private static final Logger LOG = LoggerFactory.getLogger(ProgressUpdater.class);

	private static final int UPDATE_INTERVAL_MS = 1000;

	private final ProgressPanel progressPane;
	private final Consumer<InternalTask> cancelCallback;
	private final ExecutorService bgExecutor = Executors.newSingleThreadExecutor(Utils.simpleThreadFactory("jadx-progress"));
	private final BlockingQueue<InternalTask> tasks = new DelayQueue<>();

	public ProgressUpdater(ProgressPanel progressPane, Consumer<InternalTask> cancelCallback) {
		this.progressPane = progressPane;
		this.cancelCallback = cancelCallback;
		this.bgExecutor.execute(this::updateLoop);
	}

	public void addTask(InternalTask task) {
		if (task.getBgTask().isSilent()) {
			return;
		}
		scheduleNextUpdate(task);
	}

	public void taskComplete(InternalTask task) {
		task.setNextUpdate(0);
		updateProgress(task);
	}

	private void scheduleNextUpdate(InternalTask task) {
		task.setNextUpdate(System.currentTimeMillis() + UPDATE_INTERVAL_MS);
		tasks.add(task);
	}

	private void updateLoop() {
		while (true) {
			try {
				InternalTask task = tasks.take();
				if (task.isRunning()) {
					updateProgress(task);
					cancelCheck(task);
					scheduleNextUpdate(task);
				}
			} catch (Exception e) {
				LOG.warn("Error in ProgressUpdater loop", e);
			}
		}
	}

	private void updateProgress(InternalTask internalTask) {
		UiUtils.uiRun(() -> {
			IBackgroundTask bgTask = internalTask.getBgTask();
			if (internalTask.isRunning()) {
				if (internalTask.checkForFirstUpdate()) {
					progressPane.setLabel(bgTask.getTitle() + "… ");
					progressPane.setCancelButtonVisible(bgTask.canBeCanceled());
					progressPane.setVisible(true);
				}
				ITaskProgress taskProgress = bgTask.getTaskProgress();
				if (taskProgress == null) {
					int progress = internalTask.getTaskExecutor().getProgress();
					taskProgress = new TaskProgress(progress, internalTask.getJobsCount());
				}
				progressPane.setProgress(taskProgress);
				Consumer<ITaskProgress> onProgressListener = bgTask.getProgressListener();
				if (onProgressListener != null) {
					onProgressListener.accept(taskProgress);
				}
			} else {
				progressPane.reset();
				progressPane.setVisible(false);
			}
		});
	}

	private void cancelCheck(InternalTask task) {
		TaskStatus taskStatus = task.getCancelCheck().get();
		if (taskStatus == null) {
			return;
		}
		task.setStatus(taskStatus);
		UiUtils.uiRun(() -> {
			IBackgroundTask bgTask = task.getBgTask();
			progressPane.setLabel(bgTask.getTitle() + " (" + NLS.str("progress.canceling") + ")… ");
			progressPane.setCancelButtonVisible(false);
			progressPane.setIndeterminate(true);
		});
		cancelCallback.accept(task);
	}
}
