package jadx.gui.jobs;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jadx.api.utils.tasks.ITaskExecutor;
import jadx.core.utils.tasks.TaskExecutor;
import jadx.gui.utils.NLS;

/**
 * Load task: prepare data in background task and use that data in UI task
 */
public class LoadTask<T> extends CancelableBackgroundTask {
	private final String title;
	private final AtomicReference<T> taskData;
	private final Runnable bgTask;
	private final Runnable uiTask;

	public LoadTask(Supplier<T> loadBgTask, Consumer<T> uiTask) {
		this(NLS.str("progress.load"), loadBgTask, uiTask);
	}

	public LoadTask(String title, Supplier<T> loadBgTask, Consumer<T> uiTask) {
		this.title = title;
		this.taskData = new AtomicReference<>();
		this.bgTask = () -> taskData.set(loadBgTask.get());
		this.uiTask = () -> uiTask.accept(taskData.get());
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public ITaskExecutor scheduleTasks() {
		TaskExecutor executor = new TaskExecutor();
		executor.addSequentialTask(bgTask);
		return executor;
	}

	@Override
	public void onFinish(ITaskInfo taskInfo) {
		uiTask.run();
	}
}
