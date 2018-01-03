package jadx.gui.jobs;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.JadxWrapper;

public abstract class BackgroundJob {
	private static final Logger LOG = LoggerFactory.getLogger(BackgroundJob.class);

	protected final JadxWrapper wrapper;
	private final ThreadPoolExecutor executor;
	private Future<Boolean> future;

	public BackgroundJob(JadxWrapper wrapper, int threadsCount) {
		this.wrapper = wrapper;
		this.executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadsCount);
	}

	public synchronized Future<Boolean> process() {
		if (future != null) {
			return future;
		}
		ExecutorService shutdownExecutor = Executors.newSingleThreadExecutor();
		FutureTask<Boolean> task = new ShutdownTask();
		shutdownExecutor.execute(task);
		shutdownExecutor.shutdown();
		future = task;
		return future;
	}

	private class ShutdownTask extends FutureTask<Boolean> {
		public ShutdownTask() {
			super(new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					runJob();
					executor.shutdown();
					return executor.awaitTermination(5, TimeUnit.MINUTES);
				}
			});
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			executor.shutdownNow();
			return super.cancel(mayInterruptIfRunning);
		}
	}

	protected abstract void runJob();

	public abstract String getInfoString();

	protected void addTask(Runnable runnable) {
		executor.execute(runnable);
	}

	public void processAndWait() {
		try {
			process().get();
		} catch (Exception e) {
			LOG.error("BackgroundJob.processAndWait failed", e);
		}
	}

	public synchronized boolean isComplete() {
		try {
			return future != null && future.isDone();
		} catch (Exception e) {
			LOG.error("BackgroundJob.isComplete failed", e);
			return false;
		}
	}

	public int getProgress() {
		return (int) (executor.getCompletedTaskCount() * 100 / (double) executor.getTaskCount());
	}
}
