package jadx.gui.jobs;

import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

public interface IBackgroundTask extends Cancelable {

	String getTitle();

	/**
	 * Jobs to run in parallel
	 */
	List<? extends Runnable> scheduleJobs();

	/**
	 * Called on executor thread after the all jobs finished.
	 */
	default void onDone(ITaskInfo taskInfo) {
	}

	/**
	 * Executed on the Event Dispatch Thread after the all jobs finished.
	 */
	default void onFinish(ITaskInfo taskInfo) {
	}

	default boolean canBeCanceled() {
		return false;
	}

	/**
	 * Global (for all jobs) time limit in milliseconds (0 - to disable).
	 */
	default int timeLimit() {
		return 0;
	}

	/**
	 * Executor will check memory usage on every tick and cancel job if no free memory available.
	 */
	default boolean checkMemoryUsage() {
		return false;
	}

	/**
	 * Get task progress (Optional)
	 */
	default @Nullable ITaskProgress getTaskProgress() {
		return null;
	}

	/**
	 * Return progress notifications listener (use executor tick rate and thread) (Optional)
	 */
	default @Nullable Consumer<ITaskProgress> getProgressListener() {
		return null;
	}
}
