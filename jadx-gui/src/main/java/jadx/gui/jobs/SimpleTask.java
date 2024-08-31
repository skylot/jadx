package jadx.gui.jobs;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import jadx.api.utils.tasks.ITaskExecutor;
import jadx.core.utils.tasks.TaskExecutor;

/**
 * Simple not cancelable task with memory check
 */
public class SimpleTask implements IBackgroundTask {
	private final String title;
	private final List<Runnable> jobs;
	private final @Nullable Consumer<TaskStatus> onFinish;

	public SimpleTask(String title, Runnable run) {
		this(title, Collections.singletonList(run), null);
	}

	public SimpleTask(String title, Runnable run, Runnable onFinish) {
		this(title, Collections.singletonList(run), s -> onFinish.run());
	}

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

	public List<Runnable> getJobs() {
		return jobs;
	}

	public @Nullable Consumer<TaskStatus> getOnFinish() {
		return onFinish;
	}

	@Override
	public ITaskExecutor scheduleTasks() {
		TaskExecutor executor = new TaskExecutor();
		executor.addParallelTasks(jobs);
		return executor;
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
