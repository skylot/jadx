package jadx.gui.jobs;

import jadx.api.utils.tasks.ITaskExecutor;
import jadx.core.utils.tasks.TaskExecutor;

/**
 * Simple and short task, will not show progress
 */
public class SilentTask extends CancelableBackgroundTask {
	private final Runnable task;

	public SilentTask(Runnable backgroundTask) {
		this.task = backgroundTask;
	}

	@Override
	public boolean isSilent() {
		return true;
	}

	@Override
	public String getTitle() {
		return "<silent>";
	}

	@Override
	public ITaskExecutor scheduleTasks() {
		TaskExecutor executor = new TaskExecutor();
		executor.addSequentialTask(task);
		return executor;
	}
}
