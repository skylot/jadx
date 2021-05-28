package jadx.gui.jobs;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

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

	public void execute(String title, List<Runnable> backgroundJobs, Runnable onFinishUiRunnable) {
		execute(new SimpleTask(title, backgroundJobs, onFinishUiRunnable));
	}

	public void execute(String title, Runnable backgroundRunnable, Runnable onFinishUiRunnable) {
		execute(new SimpleTask(title, backgroundRunnable, onFinishUiRunnable));
	}

	private ThreadPoolExecutor makeTaskQueueExecutor() {
		return (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
	}

	private final class TaskWorker extends SwingWorker<TaskStatus, Void> {
		private final IBackgroundTask task;
		private long jobsCount;
		private TaskStatus status = TaskStatus.WAIT;

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

			if (runJobs()) {
				status = TaskStatus.COMPLETE;
			}
			return status;
		}

		private boolean runJobs() throws InterruptedException {
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
			return waitTermination(executor);
		}

		@SuppressWarnings("BusyWait")
		private boolean waitTermination(ThreadPoolExecutor executor) throws InterruptedException {
			BooleanSupplier cancelCheck = buildCancelCheck();
			try {
				while (true) {
					if (executor.isTerminated()) {
						return true;
					}
					if (cancelCheck.getAsBoolean()) {
						performCancel(executor);
						return false;
					}
					setProgress(calcProgress(executor.getCompletedTaskCount()));
					Thread.sleep(1000);
				}
			} catch (InterruptedException e) {
				LOG.debug("Task wait interrupted");
				status = TaskStatus.CANCEL_BY_USER;
				performCancel(executor);
				return false;
			} catch (Exception e) {
				LOG.error("Task wait aborted by exception", e);
				performCancel(executor);
				return false;
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

		private boolean isSimpleTask() {
			return task.timeLimit() == 0 && !task.checkMemoryUsage();
		}

		private boolean simpleCancelCheck() {
			if (isCancelled() || Thread.currentThread().isInterrupted()) {
				LOG.debug("Task '{}' canceled", task.getTitle());
				status = TaskStatus.CANCEL_BY_USER;
				return true;
			}
			return false;
		}

		private BooleanSupplier buildCancelCheck() {
			if (isSimpleTask()) {
				return this::simpleCancelCheck;
			}
			long waitUntilTime = task.timeLimit() == 0 ? 0 : System.currentTimeMillis() + task.timeLimit();
			boolean checkMemoryUsage = task.checkMemoryUsage();
			return () -> {
				if (waitUntilTime != 0 && waitUntilTime < System.currentTimeMillis()) {
					LOG.debug("Task '{}' execution timeout, force cancel", task.getTitle());
					status = TaskStatus.CANCEL_BY_TIMEOUT;
					return true;
				}
				if (checkMemoryUsage && !UiUtils.isFreeMemoryAvailable()) {
					LOG.debug("Task '{}' memory limit reached, force cancel", task.getTitle());
					status = TaskStatus.CANCEL_BY_MEMORY;
					return true;
				}
				return simpleCancelCheck();
			};
		}

		private int calcProgress(long done) {
			return Math.round(done * 100 / (float) jobsCount);
		}

		@Override
		protected void done() {
			progressPane.setVisible(false);
			task.onFinish(status);
		}
	}

	private static final class SimpleTask implements IBackgroundTask {
		private final String title;
		private final List<Runnable> jobs;
		private final Runnable onFinish;

		public SimpleTask(String title, List<Runnable> jobs, @Nullable Runnable onFinish) {
			this.title = title;
			this.jobs = jobs;
			this.onFinish = onFinish;
		}

		public SimpleTask(String title, Runnable job, @Nullable Runnable onFinish) {
			this(title, Collections.singletonList(job), onFinish);
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
		public void onFinish(TaskStatus status) {
			if (onFinish != null) {
				onFinish.run();
			}
		}
	}
}
