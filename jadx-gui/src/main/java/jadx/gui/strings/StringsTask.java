package jadx.gui.strings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.utils.tasks.ITaskExecutor;
import jadx.core.utils.tasks.TaskExecutor;
import jadx.gui.jobs.BackgroundExecutor;
import jadx.gui.jobs.CancelableBackgroundTask;
import jadx.gui.jobs.ITaskInfo;
import jadx.gui.jobs.ITaskProgress;
import jadx.gui.jobs.TaskProgress;
import jadx.gui.jobs.TaskStatus;
import jadx.gui.strings.providers.StringsProviderDelegate;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;

public class StringsTask extends CancelableBackgroundTask {

	private static final Logger LOG = LoggerFactory.getLogger(StringsTask.class);

	private final Consumer<StringResult> resultsListener;
	private final AtomicInteger resultsCount;
	private final List<StringsJob> jobs;
	private final BiConsumer<ITaskInfo, Boolean> onFinish;
	private final TaskProgress taskProgress;
	private final BackgroundExecutor backgroundExecutor;

	private Consumer<ITaskProgress> progressListener;
	private Future<TaskStatus> future;
	private int resultsLimit;

	public StringsTask(MainWindow mainWindow, Consumer<StringResult> resultsListener, BiConsumer<ITaskInfo, Boolean> onFinish) {
		this.resultsListener = resultsListener;
		this.jobs = new ArrayList<>();
		this.onFinish = onFinish;
		this.taskProgress = new TaskProgress();
		this.backgroundExecutor = mainWindow.getBackgroundExecutor();
		this.resultsCount = new AtomicInteger(0);
	}

	@Override
	public String getTitle() {
		return NLS.str("strings.task");
	}

	@Override
	public ITaskExecutor scheduleTasks() {
		final TaskExecutor executor = new TaskExecutor();
		executor.addParallelTasks(jobs);
		return executor;
	}

	@Override
	public void onFinish(ITaskInfo task) {
		final boolean complete = !isCanceled()
				&& task.getStatus() == TaskStatus.COMPLETE
				&& task.getJobsComplete() == task.getJobsCount();
		this.onFinish.accept(task, complete);
	}

	@Override
	public boolean checkMemoryUsage() {
		return true;
	}

	@Override
	public @NotNull ITaskProgress getTaskProgress() {
		taskProgress.updateProgress(jobs.stream().mapToInt(s -> s.getDelegate().progress()).sum());
		return taskProgress;
	}

	public synchronized boolean addResult(final StringResult resultNode) {
		if (isCanceled()) {
			// ignore new results after cancel
			return true;
		}

		this.resultsListener.accept(resultNode);
		final boolean atResultsLimit = resultsLimit != 0 && resultsCount.incrementAndGet() >= resultsLimit;
		if (atResultsLimit) {
			cancel();
		}
		return atResultsLimit;
	}

	public synchronized void fetchResults() {
		if (future != null) {
			throw new IllegalStateException("Previous task not yet finished");
		}
		resetCancel();
		resultsCount.set(0);
		taskProgress.updateTotal(jobs.stream().mapToInt(s -> s.getDelegate().total()).sum());
		future = backgroundExecutor.executeWithFuture(this);
	}

	public synchronized void waitTask() {
		if (future != null) {
			try {
				future.get(200, TimeUnit.MILLISECONDS);
			} catch (TimeoutException e) {
				LOG.debug("Search task wait timeout");
			} catch (Exception e) {
				LOG.warn("Search task wait error", e);
			} finally {
				future.cancel(true);
				future = null;
			}
		}

	}

	public void addProviderJob(final StringsProviderDelegate delegate) {
		this.jobs.add(new StringsJob(this, delegate));
	}

	public void setResultsLimit(final int limit) {
		this.resultsLimit = limit;
	}

	public void setProgressListener(final Consumer<ITaskProgress> progressListener) {
		this.progressListener = progressListener;
	}

	@Override
	public @Nullable Consumer<ITaskProgress> getProgressListener() {
		return this.progressListener;
	}

}
