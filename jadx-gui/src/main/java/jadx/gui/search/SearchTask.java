package jadx.gui.search;

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

import jadx.gui.jobs.BackgroundExecutor;
import jadx.gui.jobs.CancelableBackgroundTask;
import jadx.gui.jobs.ITaskInfo;
import jadx.gui.jobs.ITaskProgress;
import jadx.gui.jobs.TaskProgress;
import jadx.gui.jobs.TaskStatus;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;

public class SearchTask extends CancelableBackgroundTask {
	private static final Logger LOG = LoggerFactory.getLogger(SearchTask.class);

	private final BackgroundExecutor backgroundExecutor;
	private final Consumer<JNode> resultsListener;
	private final BiConsumer<ITaskInfo, Boolean> onFinish;
	private final List<SearchJob> jobs = new ArrayList<>();
	private final TaskProgress taskProgress = new TaskProgress();

	private final AtomicInteger resultsCount = new AtomicInteger(0);
	private int resultsLimit;
	private Future<TaskStatus> future;

	private Consumer<ITaskProgress> progressListener;

	public SearchTask(MainWindow mainWindow, Consumer<JNode> results, BiConsumer<ITaskInfo, Boolean> onFinish) {
		this.backgroundExecutor = mainWindow.getBackgroundExecutor();
		this.resultsListener = results;
		this.onFinish = onFinish;
	}

	public void addProviderJob(ISearchProvider provider) {
		jobs.add(new SearchJob(this, provider));
	}

	public void setResultsLimit(int limit) {
		this.resultsLimit = limit;
	}

	public synchronized void fetchResults() {
		if (future != null) {
			throw new IllegalStateException("Previous task not yet finished");
		}
		resetCancel();
		resultsCount.set(0);
		taskProgress.updateTotal(jobs.stream().mapToInt(s -> s.getProvider().total()).sum());
		future = backgroundExecutor.execute(this);
	}

	public synchronized boolean addResult(JNode resultNode) {
		if (isCanceled()) {
			// ignore new results after cancel
			return true;
		}
		this.resultsListener.accept(resultNode);
		if (resultsLimit != 0 && resultsCount.incrementAndGet() >= resultsLimit) {
			cancel();
			return true;
		}
		return false;
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

	@Override
	public String getTitle() {
		return NLS.str("search_dialog.tip_searching");
	}

	@Override
	public List<? extends Runnable> scheduleJobs() {
		return jobs;
	}

	@Override
	public void onFinish(ITaskInfo task) {
		boolean complete = !isCanceled()
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
		taskProgress.updateProgress(jobs.stream().mapToInt(s -> s.getProvider().progress()).sum());
		return taskProgress;
	}

	public void setProgressListener(Consumer<ITaskProgress> progressListener) {
		this.progressListener = progressListener;
	}

	@Override
	public @Nullable Consumer<ITaskProgress> getProgressListener() {
		return this.progressListener;
	}

	@Override
	public int getCancelTimeoutMS() {
		return 0;
	}

	@Override
	public int getShutdownTimeoutMS() {
		return 10;
	}
}
