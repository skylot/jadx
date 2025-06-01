package jadx.core.utils.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.Nullable;

import jadx.api.JadxArgs;
import jadx.api.utils.tasks.ITaskExecutor;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class TaskExecutor implements ITaskExecutor {

	private enum ExecType {
		PARALLEL,
		SEQUENTIAL,
	}

	private static final class ExecStage {
		private final ExecType type;
		private final List<? extends Runnable> tasks;

		private ExecStage(ExecType type, List<? extends Runnable> tasks) {
			this.type = type;
			this.tasks = tasks;
		}

		public ExecType getType() {
			return type;
		}

		public List<? extends Runnable> getTasks() {
			return tasks;
		}
	}

	private final List<ExecStage> stages = new ArrayList<>();
	private final AtomicInteger threadsCount = new AtomicInteger(JadxArgs.DEFAULT_THREADS_COUNT);
	private final AtomicInteger progress = new AtomicInteger(0);
	private final AtomicBoolean running = new AtomicBoolean(false);
	private final AtomicBoolean terminating = new AtomicBoolean(false);
	private int tasksCount = 0;
	private @Nullable ExecutorService executor;

	@Override
	public void addParallelTasks(List<? extends Runnable> parallelTasks) {
		if (parallelTasks.isEmpty()) {
			return;
		}
		tasksCount += parallelTasks.size();
		stages.add(new ExecStage(ExecType.PARALLEL, parallelTasks));
	}

	@Override
	public void addSequentialTasks(List<? extends Runnable> seqTasks) {
		if (seqTasks.isEmpty()) {
			return;
		}
		tasksCount += seqTasks.size();
		stages.add(new ExecStage(ExecType.SEQUENTIAL, seqTasks));
	}

	@Override
	public void addSequentialTask(Runnable seqTask) {
		addSequentialTasks(Collections.singletonList(seqTask));
	}

	@Override
	public int getThreadsCount() {
		return threadsCount.get();
	}

	@Override
	public void setThreadsCount(int count) {
		threadsCount.set(count);
	}

	@Override
	public int getTasksCount() {
		return tasksCount;
	}

	@Override
	public int getProgress() {
		return progress.get();
	}

	@Override
	public void execute() {
		if (running.get() || executor != null) {
			throw new IllegalStateException("Already executing");
		}
		running.set(true);
		progress.set(0);
		terminating.set(false);
		executor = Executors.newFixedThreadPool(1, Utils.simpleThreadFactory("task-s"));
		executor.execute(this::runStages);
		executor.shutdown();
	}

	@Override
	public void terminate() {
		terminating.set(true);
	}

	@Override
	public boolean isTerminating() {
		return terminating.get();
	}

	@Override
	public boolean isRunning() {
		return running.get();
	}

	@Override
	public @Nullable ExecutorService getInternalExecutor() {
		return executor;
	}

	@Override
	public void awaitTermination() {
		if (executor == null || !running.get()) {
			// already terminated
			return;
		}
		awaitExecutorTermination(executor);
	}

	private void runStages() {
		try {
			for (ExecStage stage : stages) {
				int threads = Math.min(stage.getTasks().size(), threadsCount.get());
				if (stage.getType() == ExecType.SEQUENTIAL || threads == 1) {
					for (Runnable task : stage.getTasks()) {
						wrapTask(task);
					}
				} else {
					ExecutorService parallelExecutor = Executors.newFixedThreadPool(
							threads, Utils.simpleThreadFactory("task-p"));
					for (Runnable task : stage.getTasks()) {
						parallelExecutor.execute(() -> wrapTask(task));
					}
					parallelExecutor.shutdown();
					awaitExecutorTermination(parallelExecutor);
				}
				if (terminating.get()) {
					break;
				}
			}
		} finally {
			running.set(false);
			executor = null;
		}
	}

	private void wrapTask(Runnable task) {
		if (terminating.get()) {
			return;
		}
		task.run();
		progress.incrementAndGet();
	}

	public static void awaitExecutorTermination(ExecutorService executor) {
		try {
			boolean complete = executor.awaitTermination(10, TimeUnit.DAYS);
			if (!complete) {
				throw new JadxRuntimeException("Executor timeout");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
