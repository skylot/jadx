package jadx.gui.jobs;

import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

/**
 * Simple not cancelable task with memory check
 */
public class SimpleTask implements IBackgroundTask {
	private final String title;
	private final List<Runnable> jobs;
	private final Consumer<TaskStatus> onFinish;

	public SimpleTask(String title, List<Runnable> jobs) {
		this(title, jobs, null);
	}

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
	public void onFinish(ITaskInfo taskInfo) {
		if (onFinish != null) {
			onFinish.accept(taskInfo.getStatus());
		}
	}

	@Override
	public boolean checkMemoryUsage() {
		return true;
	}

	@Override
	public boolean canBeCanceled() {
		return false;
	}

	@Override
	public boolean isCanceled() {
		return false;
	}

	@Override
	public void cancel() {
	}
}
