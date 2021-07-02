package jadx.gui.jobs;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.SwingWorker;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.ui.MainWindow;
import jadx.gui.ui.ProgressPanel;
import jadx.gui.utils.UiUtils;

/**
 * Class for run tasks in background with progress bar indication.
 * Use instance created in {@link MainWindow}.
 */
public class BackgroundExecutor {
	private static final Logger LOG = LoggerFactory.getLogger(BackgroundExecutor.class);

	private final MainWindow mainWindow;
	private final ProgressPanel progressPane;

	private ThreadPoolExecutor taskQueueExecutor;

	public BackgroundExecutor(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
		this.progressPane = mainWindow.getProgressPane();
		this.taskQueueExecutor = makeTaskQueueExecutor();
	}

	public Future<TaskStatus> execute(IBackgroundTask task) {
		TaskWorker taskWorker = new TaskWorker(task);
		taskQueueExecutor.execute(() -> {
			taskWorker.init();
			taskWorker.run();
		});
		return taskWorker;
	}

	public void cancelAll() {
		try {
			taskQueueExecutor.shutdownNow();
			boolean complete = taskQueueExecutor.awaitTermination(2, TimeUnit.SECONDS);
			LOG.debug("Background task executor terminated with status: {}", complete ? "complete" : "interrupted");
		} catch (Exception e) {
			LOG.error("Error terminating task executor", e);
		} finally {
			taskQueueExecutor = makeTaskQueueExecutor();
		}
	}

	public void execute(String title, List<Runnable> backgroundJobs, Consumer<TaskStatus> onFinishUiRunnable) {
		execute(new SimpleTask(title, backgroundJobs, onFinishUiRunnable));
	}

	public void execute(String title, Runnable backgroundRunnable, Consumer<TaskStatus> onFinishUiRunnable) {
		execute(new SimpleTask(title, Collections.singletonList(backgroundRunnable), onFinishUiRunnable));
	}

	public void execute(String title, Runnable backgroundRunnable) {
		execute(new SimpleTask(title, Collections.singletonList(backgroundRunnable), null));
	}

	private ThreadPoolExecutor makeTaskQueueExecutor() {
		return (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
	}

	private final class TaskWorker extends SwingWorker<TaskStatus, Void> {
		private final IBackgroundTask task;
		private TaskStatus status = TaskStatus.WAIT;
		private long jobsCount;
		private long jobsComplete;

		public TaskWorker(IBackgroundTask task) {
			this.task = task;
		}

		public void init() {
			addPropertyChangeListener(progressPane);
			progressPane.reset();
		}

		@Override
		protected TaskStatus doInBackground() throws Exception {
			progressPane.changeLabel(this, task.getTitle() + "… ");
			progressPane.changeCancelBtnVisible(this, task.canBeCanceled());
			progressPane.changeVisibility(this, true);

			runJobs();
			return status;
		}

		private void runJobs() throws InterruptedException {
			List<Runnable> jobs = task.scheduleJobs();
			jobsCount = jobs.size();
			LOG.debug("Starting background task '{}', jobs count: {}, time limit: {} ms, memory check: {}",
					task.getTitle(), jobsCount, task.timeLimit(), task.checkMemoryUsage());
			status = TaskStatus.STARTED;
			int threadsCount = mainWindow.getSettings().getThreadsCount();
			ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadsCount);
			for (Runnable job : jobs) {
				executor.execute(job);
			}
			executor.shutdown();
			status = waitTermination(executor);
			jobsComplete = executor.getCompletedTaskCount();
		}

		@SuppressWarnings("BusyWait")
		private TaskStatus waitTermination(ThreadPoolExecutor executor) throws InterruptedException {
			Supplier<TaskStatus> cancelCheck = buildCancelCheck();
			try {
				while (true) {
					if (executor.isTerminated()) {
						return TaskStatus.COMPLETE;
					}
					TaskStatus cancelStatus = cancelCheck.get();
					if (cancelStatus != null) {
						performCancel(executor);
						return cancelStatus;
					}
					setProgress(calcProgress(executor.getCompletedTaskCount()));
					Thread.sleep(500);
				}
			} catch (InterruptedException e) {
				LOG.debug("Task wait interrupted");
				performCancel(executor);
				return TaskStatus.CANCEL_BY_USER;
			} catch (Exception e) {
				LOG.error("Task wait aborted by exception", e);
				performCancel(executor);
				return TaskStatus.ERROR;
			}
		}

		private void performCancel(ThreadPoolExecutor executor) throws InterruptedException {
			progressPane.changeLabel(this, task.getTitle() + " (Canceling)… ");
			progressPane.changeIndeterminate(this, true);
			// force termination
			executor.shutdownNow();
			boolean complete = executor.awaitTermination(5, TimeUnit.SECONDS);
			LOG.debug("Task cancel complete: {}", complete);
		}

		private Supplier<TaskStatus> buildCancelCheck() {
			long waitUntilTime = task.timeLimit() == 0 ? 0 : System.currentTimeMillis() + task.timeLimit();
			boolean checkMemoryUsage = task.checkMemoryUsage();
			return () -> {
				if (waitUntilTime != 0 && waitUntilTime < System.currentTimeMillis()) {
					LOG.error("Task '{}' execution timeout, force cancel", task.getTitle());
					return TaskStatus.CANCEL_BY_TIMEOUT;
				}
				if (checkMemoryUsage && !UiUtils.isFreeMemoryAvailable()) {
					LOG.error("Task '{}' memory limit reached, force cancel", task.getTitle());
					return TaskStatus.CANCEL_BY_MEMORY;
				}
				if (isCancelled() || Thread.currentThread().isInterrupted()) {
					LOG.warn("Task '{}' canceled", task.getTitle());
					return TaskStatus.CANCEL_BY_USER;
				}
				return null;
			};
		}

		private int calcProgress(long done) {
			return Math.round(done * 100 / (float) jobsCount);
		}

		@Override
		protected void done() {
			progressPane.setVisible(false);
			task.onFinish(status, jobsCount - jobsComplete);
		}
	}

	private static final class SimpleTask implements IBackgroundTask {
		private final String title;
		private final List<Runnable> jobs;
		private final Consumer<TaskStatus> onFinish;

		public SimpleTask(String title, List<Runnable> jobs, @Nullable Consumer<TaskStatus> onFinish) {
			this.title = title;
			this.jobs = jobs;
			this.onFinish = onFinish;
		}

		@Override
		public String getTitle() {
			return title;
		}

		@Override
		public List<Runnable> scheduleJobs() {
			return jobs;
		}

		@Override
		public void onFinish(TaskStatus status, long l) {
			if (onFinish != null) {
				onFinish.accept(status);
			}
		}

		@Override
		public boolean checkMemoryUsage() {
			return true;
		}
	}
}
