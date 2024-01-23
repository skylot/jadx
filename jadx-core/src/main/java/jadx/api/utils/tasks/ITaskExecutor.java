package jadx.api.utils.tasks;

import java.util.List;
import java.util.concurrent.ExecutorService;

import org.jetbrains.annotations.Nullable;

/**
 * Schedule and execute tasks combined into stages
 * with parallel or sequential execution (similar to the fork-join pattern).
 */
public interface ITaskExecutor {

	/**
	 * Add parallel stage with provided tasks
	 */
	void addParallelTasks(List<? extends Runnable> parallelTasks);

	/**
	 * Add sequential stage with provided tasks
	 */
	void addSequentialTasks(List<? extends Runnable> seqTasks);

	/**
	 * Add sequential stage with a single task
	 */
	void addSequentialTask(Runnable task);

	/**
	 * Scheduled tasks count
	 */
	int getTasksCount();

	/**
	 * Set threads count for parallel stage.
	 * Can be changed during execution.
	 * Defaults to half of processors count.
	 */
	void setThreadsCount(int threadsCount);

	int getThreadsCount();

	/**
	 * Start tasks execution.
	 */
	void execute();

	int getProgress();

	/**
	 * Not started tasks will be not executed after this method invocation.
	 */
	void terminate();

	boolean isTerminating();

	boolean isRunning();

	/**
	 * Block until execution is finished
	 */
	void awaitTermination();

	/**
	 * Return internal executor service.
	 */
	@Nullable
	ExecutorService getInternalExecutor();
}
