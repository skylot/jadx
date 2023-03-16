package jadx.gui.jobs;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.settings.JadxSettings;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.panel.ProgressPanel;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

import static jadx.gui.utils.UiUtils.calcProgress;

/**
 * Class for run tasks in background with progress bar indication.
 * Use instance created in {@link MainWindow}.
 */
public class BackgroundExecutor {
	private static final Logger LOG = LoggerFactory.getLogger(BackgroundExecutor.class);

	private final JadxSettings settings;
	private final ProgressPanel progressPane;

	private ThreadPoolExecutor taskQueueExecutor;
	private final Map<Long, IBackgroundTask> taskRunning = new ConcurrentHashMap<>();
	private final AtomicLong idSupplier = new AtomicLong(0);

	public BackgroundExecutor(JadxSettings settings, ProgressPanel progressPane) {
		this.settings = Objects.requireNonNull(settings);
		this.progressPane = Objects.requireNonNull(progressPane);
		reset();
	}

	public synchronized Future<TaskStatus> execute(IBackgroundTask task) {
		long id = idSupplier.incrementAndGet();
		TaskWorker taskWorker = new TaskWorker(id, task);
		taskRunning.put(id, task);
		taskQueueExecutor.execute(() -> {
			taskWorker.init();
			taskWorker.run();
		});
		return taskWorker;
	}

	public synchronized void cancelAll() {
		try {
			taskRunning.values().forEach(Cancelable::cancel);
			taskQueueExecutor.shutdownNow();
			boolean complete = taskQueueExecutor.awaitTermination(3, TimeUnit.SECONDS);
			if (complete) {
				LOG.debug("Background task executor canceled successfully");
			} else {
				String taskNames = taskRunning.values().stream()
						.map(IBackgroundTask::getTitle)
						.collect(Collectors.joining(", "));
				LOG.debug("Background task executor cancel failed. Running tasks: {}", taskNames);
			}
		} catch (Exception e) {
			LOG.error("Error terminating task executor", e);
		} finally {
			reset();
		}
	}

	public void execute(String title, List<Runnable> backgroundJobs, Consumer<TaskStatus> onFinishUiRunnable) {
		execute(new SimpleTask(title, backgroundJobs, onFinishUiRunnable));
	}

	public void execute(String title, Runnable backgroundRunnable, Consumer<TaskStatus> onFinishUiRunnable) {
		execute(new SimpleTask(title, Collections.singletonList(backgroundRunnable), onFinishUiRunnable));
	}

	public Future<TaskStatus> execute(String title, Runnable backgroundRunnable) {
		return execute(new SimpleTask(title, Collections.singletonList(backgroundRunnable)));
	}

	private synchronized void reset() {
		taskQueueExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
		taskRunning.clear();
		idSupplier.set(0);
	}

	private void taskComplete(long id) {
		taskRunning.remove(id);
	}

	private final class TaskWorker extends SwingWorker<TaskStatus, Void> implements ITaskInfo {
		private final long id;
		private final IBackgroundTask task;
		private ThreadPoolExecutor executor;
		private TaskStatus status = TaskStatus.WAIT;
		private long jobsCount;
		private long jobsComplete;
		private long time;

		public TaskWorker(long id, IBackgroundTask task) {
			this.id = id;
			this.task = task;
		}

		public void init() {
			addPropertyChangeListener(progressPane);
			SwingUtilities.invokeLater(() -> {
				progressPane.reset();
				if (task.getTaskProgress() != null) {
					progressPane.setIndeterminate(false);
				}
			});
		}

		@Override
		protected TaskStatus doInBackground() throws Exception {
			progressPane.changeLabel(this, task.getTitle() + "… ");
			progressPane.changeCancelBtnVisible(this, task.canBeCanceled());
			try {
				runJobs();
			} finally {
				try {
					task.onDone(this);
					// treat UI task operations as part of the task to not mix with others
					UiUtils.uiRunAndWait(() -> {
						progressPane.setVisible(false);
						task.onFinish(this);
					});
				} finally {
					taskComplete(id);
					progressPane.changeVisibility(this, false);
				}
			}
			return status;
		}

		private void runJobs() throws InterruptedException {
			List<? extends Runnable> jobs = task.scheduleJobs();
			jobsCount = jobs.size();
			LOG.debug("Starting background task '{}', jobs count: {}, time limit: {} ms, memory check: {}",
					task.getTitle(), jobsCount, task.timeLimit(), task.checkMemoryUsage());
			if (jobsCount != 1) {
				progressPane.changeVisibility(this, true);
			}
			status = TaskStatus.STARTED;
			int threadsCount = settings.getThreadsCount();
			executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadsCount);
			for (Runnable job : jobs) {
				executor.execute(job);
			}
			executor.shutdown();
			long startTime = System.currentTimeMillis();
			status = waitTermination(executor, buildCancelCheck(startTime));
			time = System.currentTimeMillis() - startTime;
			jobsComplete = executor.getCompletedTaskCount();
		}

		@SuppressWarnings("BusyWait")
		private TaskStatus waitTermination(ThreadPoolExecutor executor, Supplier<TaskStatus> cancelCheck) throws InterruptedException {
			try {
				int k = 0;
				while (true) {
					if (executor.isTerminated()) {
						return TaskStatus.COMPLETE;
					}
					TaskStatus cancelStatus = cancelCheck.get();
					if (cancelStatus != null) {
						performCancel(executor);
						return cancelStatus;
					}
					if (k < 10) {
						// faster update for short tasks
						Thread.sleep(200);
						if (k == 5) {
							updateProgress(executor);
						}
					} else {
						updateProgress(executor);
						Thread.sleep(1000);
					}
					if (jobsCount == 1 && k == 5) {
						// small delay before show progress to reduce blinking on short tasks
						progressPane.changeVisibility(this, true);
					}
					k++;
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

		private void updateProgress(ThreadPoolExecutor executor) {
			Consumer<ITaskProgress> onProgressListener = task.getProgressListener();
			ITaskProgress taskProgress = task.getTaskProgress();
			if (taskProgress == null) {
				setProgress(calcProgress(executor.getCompletedTaskCount(), jobsCount));
				if (onProgressListener != null) {
					onProgressListener.accept(new TaskProgress(executor.getCompletedTaskCount(), jobsCount));
				}
			} else {
				setProgress(calcProgress(taskProgress));
				if (onProgressListener != null) {
					onProgressListener.accept(taskProgress);
				}
			}
		}

		private void performCancel(ThreadPoolExecutor executor) throws InterruptedException {
			progressPane.changeLabel(this, task.getTitle() + " (" + NLS.str("progress.canceling") + ")… ");
			progressPane.changeIndeterminate(this, true);
			// force termination
			task.cancel();
			executor.shutdown();
			int cancelTimeout = task.getCancelTimeoutMS();
			if (cancelTimeout != 0) {
				if (executor.awaitTermination(cancelTimeout, TimeUnit.MILLISECONDS)) {
					LOG.debug("Task cancel complete");
					return;
				}
			}
			LOG.debug("Forcing tasks cancel");
			executor.shutdownNow();
			boolean complete = executor.awaitTermination(task.getShutdownTimeoutMS(), TimeUnit.MILLISECONDS);
			LOG.debug("Forced task cancel status: {}",
					complete ? "success" : "fail, still active: " + executor.getActiveCount());
		}

		private Supplier<TaskStatus> buildCancelCheck(long startTime) {
			long waitUntilTime = task.timeLimit() == 0 ? 0 : startTime + task.timeLimit();
			boolean checkMemoryUsage = task.checkMemoryUsage();
			return () -> {
				if (task.isCanceled()) {
					return TaskStatus.CANCEL_BY_USER;
				}
				if (waitUntilTime != 0 && waitUntilTime < System.currentTimeMillis()) {
					LOG.error("Task '{}' execution timeout, force cancel", task.getTitle());
					return TaskStatus.CANCEL_BY_TIMEOUT;
				}
				if (isCancelled() || Thread.currentThread().isInterrupted()) {
					LOG.warn("Task '{}' canceled", task.getTitle());
					return TaskStatus.CANCEL_BY_USER;
				}
				if (checkMemoryUsage && !UiUtils.isFreeMemoryAvailable()) {
					LOG.info("Memory usage: {}", UiUtils.memoryInfo());
					if (executor.getCorePoolSize() == 1) {
						LOG.error("Task '{}' memory limit reached, force cancel", task.getTitle());
						return TaskStatus.CANCEL_BY_MEMORY;
					}
					LOG.warn("Low memory, reduce processing threads count to 1");
					// reduce thread count and continue
					executor.setCorePoolSize(1);
					System.gc();
					UiUtils.sleep(1000); // wait GC
					if (!UiUtils.isFreeMemoryAvailable()) {
						LOG.error("Task '{}' memory limit reached (after GC), force cancel", task.getTitle());
						return TaskStatus.CANCEL_BY_MEMORY;
					}
				}
				return null;
			};
		}

		@Override
		public TaskStatus getStatus() {
			return status;
		}

		@Override
		public long getJobsCount() {
			return jobsCount;
		}

		@Override
		public long getJobsComplete() {
			return jobsComplete;
		}

		@Override
		public long getJobsSkipped() {
			return jobsCount - jobsComplete;
		}

		@Override
		public long getTime() {
			return time;
		}

		@Override
		public String toString() {
			return "TaskWorker{status=" + status
					+ ", jobsCount=" + jobsCount
					+ ", jobsComplete=" + jobsComplete
					+ ", time=" + time + '}';
		}
	}
}
