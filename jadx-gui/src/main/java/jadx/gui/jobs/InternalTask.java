package jadx.gui.jobs;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.jetbrains.annotations.NotNull;

import jadx.api.utils.tasks.ITaskExecutor;

public class InternalTask implements Delayed, ITaskInfo {
	private final long id;
	private final IBackgroundTask bgTask;
	private final AtomicBoolean running = new AtomicBoolean(false);
	private final AtomicLong nextUpdate = new AtomicLong(0);
	private final AtomicBoolean firstUpdate = new AtomicBoolean(true);

	private long startTime;
	private long execTime;
	private Supplier<TaskStatus> cancelCheck;
	private TaskStatus status = TaskStatus.WAIT;
	private ITaskExecutor taskExecutor;
	private long jobsCount;
	private long jobsComplete;

	public InternalTask(long id, IBackgroundTask task) {
		this.id = id;
		this.bgTask = task;
	}

	public void taskStart(long startTime, Supplier<TaskStatus> cancelCheck) {
		this.startTime = startTime;
		this.cancelCheck = cancelCheck;
		this.status = TaskStatus.STARTED;
		this.running.set(true);
	}

	public void taskComplete() {
		this.running.set(false);
		if (status == TaskStatus.STARTED) {
			// might be already set to error or cancel
			this.status = TaskStatus.COMPLETE;
		}
		updateExecTime();
	}

	public long getId() {
		return id;
	}

	public IBackgroundTask getBgTask() {
		return bgTask;
	}

	public void setNextUpdate(long nextUpdate) {
		this.nextUpdate.set(nextUpdate);
	}

	public boolean isRunning() {
		return running.get();
	}

	public boolean checkForFirstUpdate() {
		return firstUpdate.compareAndExchange(true, false);
	}

	public Supplier<TaskStatus> getCancelCheck() {
		return cancelCheck;
	}

	public long getStartTime() {
		return startTime;
	}

	@Override
	public TaskStatus getStatus() {
		return status;
	}

	public void setStatus(TaskStatus taskStatus) {
		this.status = taskStatus;
	}

	public ITaskExecutor getTaskExecutor() {
		return taskExecutor;
	}

	public void setTaskExecutor(ITaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	@Override
	public long getJobsComplete() {
		return jobsComplete;
	}

	public void setJobsComplete(long jobsComplete) {
		this.jobsComplete = jobsComplete;
	}

	@Override
	public long getJobsCount() {
		return jobsCount;
	}

	public void setJobsCount(long jobsCount) {
		this.jobsCount = jobsCount;
	}

	@Override
	public long getJobsSkipped() {
		return jobsCount - jobsComplete;
	}

	@Override
	public long getTime() {
		return execTime;
	}

	public void updateExecTime() {
		this.execTime = System.currentTimeMillis() - startTime;
	}

	@Override
	public long getDelay(@NotNull TimeUnit unit) {
		return unit.convert(nextUpdate.get() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
	}

	@Override
	public int compareTo(@NotNull Delayed o) {
		return Long.compare(nextUpdate.get(), ((InternalTask) o).nextUpdate.get());
	}

	@Override
	public String toString() {
		return "InternalTask{" + bgTask.getTitle() + ", status=" + status
				+ ", progress=" + jobsComplete + " of " + jobsCount + '}';
	}
}
