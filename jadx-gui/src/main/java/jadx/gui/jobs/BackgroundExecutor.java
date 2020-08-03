package jadx.gui.jobs;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.swing.*;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.ui.MainWindow;
import jadx.gui.ui.ProgressPanel;

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

	public Future<Boolean> execute(IBackgroundTask task) {
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
			taskQueueExecutor.awaitTermination(1, TimeUnit.SECONDS);
		} catch (Exception e) {
			LOG.error("Error terminating task executor", e);
		} finally {
			taskQueueExecutor = makeTaskQueueExecutor();
		}
	}

	public void execute(String title, List<Runnable> backgroundJobs, Runnable onFinishUiRunnable) {
		execute(new SimpleTask(title, backgroundJobs, onFinishUiRunnable));
	}

	public void execute(String title, List<Runnable> backgroundJobs) {
		execute(new SimpleTask(title, backgroundJobs, null));
	}

	public void execute(String title, Runnable backgroundRunnable, Runnable onFinishUiRunnable) {
		execute(new SimpleTask(title, backgroundRunnable, onFinishUiRunnable));
	}

	public void execute(String title, Runnable backgroundRunnable) {
		execute(new SimpleTask(title, backgroundRunnable, null));
	}

	private ThreadPoolExecutor makeTaskQueueExecutor() {
		return (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
	}

	private final class TaskWorker extends SwingWorker<Boolean, Void> {
		private final IBackgroundTask task;
		private long jobsCount;

		public TaskWorker(IBackgroundTask task) {
			this.task = task;
		}

		public void init() {
			addPropertyChangeListener(progressPane);
			progressPane.reset();
		}

		@Override
		protected Boolean doInBackground() throws Exception {
			progressPane.changeLabel(this, task.getTitle() + "… ");
			progressPane.changeCancelBtnVisible(this, task.canBeCanceled());
			progressPane.changeVisibility(this, true);

			List<Runnable> jobs = task.scheduleJobs();
			jobsCount = jobs.size();
			LOG.debug("Starting background task '{}', jobs count: {}", task.getTitle(), jobsCount);
			if (jobsCount == 1) {
				jobs.get(0).run();
				return true;
			}
			int threadsCount = mainWindow.getSettings().getThreadsCount();
			if (threadsCount == 1) {
				return runInCurrentThread(jobs);
			}
			return runInExecutor(jobs, threadsCount);
		}

		private boolean runInCurrentThread(List<Runnable> jobs) {
			int k = 0;
			for (Runnable job : jobs) {
				job.run();
				k++;
				setProgress(calcProgress(k));
				if (isCancelled()) {
					return false;
				}
			}
			return true;
		}

		private boolean runInExecutor(List<Runnable> jobs, int threadsCount) throws InterruptedException {
			ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadsCount);
			for (Runnable job : jobs) {
				executor.execute(job);
			}
			executor.shutdown();
			return waitTermination(executor);
		}

		private boolean waitTermination(ThreadPoolExecutor executor) throws InterruptedException {
			while (true) {
				if (executor.isTerminated()) {
					return true;
				}
				if (isCancelled()) {
					executor.shutdownNow();
					progressPane.changeLabel(this, task.getTitle() + " (Canceling)… ");
					progressPane.changeIndeterminate(this, true);
					// force termination
					executor.awaitTermination(5, TimeUnit.SECONDS);
					return false;
				}
				setProgress(calcProgress(executor.getCompletedTaskCount()));
				Thread.sleep(500);
			}
		}

		private int calcProgress(long done) {
			return Math.round(done * 100 / (float) jobsCount);
		}

		@Override
		protected void done() {
			progressPane.setVisible(false);
			task.onFinish();
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
		public boolean canBeCanceled() {
			return false;
		}

		@Override
		public void onFinish() {
			if (onFinish != null) {
				onFinish.run();
			}
		}
	}
}
