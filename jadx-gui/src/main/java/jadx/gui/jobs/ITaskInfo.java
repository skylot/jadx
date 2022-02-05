package jadx.gui.jobs;

public interface ITaskInfo {
	TaskStatus getStatus();

	long getJobsCount();

	long getJobsComplete();

	long getJobsSkipped();

	long getTime();
}
