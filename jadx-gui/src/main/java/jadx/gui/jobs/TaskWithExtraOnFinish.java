package jadx.gui.jobs;

import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import jadx.api.utils.tasks.ITaskExecutor;

/**
 * Add additional `onFinish` action to the existing task
 */
public class TaskWithExtraOnFinish implements IBackgroundTask {
	private final IBackgroundTask task;
	private final Consumer<TaskStatus> extraOnFinish;

	public TaskWithExtraOnFinish(IBackgroundTask task, Runnable extraOnFinish) {
		this(task, s -> extraOnFinish.run());
	}

	public TaskWithExtraOnFinish(IBackgroundTask task, Consumer<TaskStatus> extraOnFinish) {
		this.task = Objects.requireNonNull(task);
		this.extraOnFinish = Objects.requireNonNull(extraOnFinish);
	}

	@Override
	public void onFinish(ITaskInfo taskInfo) {
		task.onFinish(taskInfo);
		extraOnFinish.accept(taskInfo.getStatus());
	}

	@Override
	public String getTitle() {
		return task.getTitle();
	}

	@Override
	public ITaskExecutor scheduleTasks() {
		return task.scheduleTasks();
	}

	@Override
	public void onDone(ITaskInfo taskInfo) {
		task.onDone(taskInfo);
	}

	@Override
	public @Nullable Consumer<ITaskProgress> getProgressListener() {
		return task.getProgressListener();
	}

	@Override
	public @Nullable ITaskProgress getTaskProgress() {
		return task.getTaskProgress();
	}

	@Override
	public boolean canBeCanceled() {
		return task.canBeCanceled();
	}

	@Override
	public boolean isCanceled() {
		return task.isCanceled();
	}

	@Override
	public void cancel() {
		task.cancel();
	}

	@Override
	public int timeLimit() {
		return task.timeLimit();
	}

	@Override
	public boolean checkMemoryUsage() {
		return task.checkMemoryUsage();
	}

	@Override
	public int getCancelTimeoutMS() {
		return task.getCancelTimeoutMS();
	}

	@Override
	public int getShutdownTimeoutMS() {
		return task.getShutdownTimeoutMS();
	}
}
