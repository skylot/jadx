package jadx.gui.jobs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeCache;
import jadx.api.JavaClass;
import jadx.gui.JadxWrapper;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

public class DecompileTask implements IBackgroundTask {
	private static final Logger LOG = LoggerFactory.getLogger(DecompileTask.class);

	private static final int CLS_LIMIT = Integer.parseInt(UiUtils.getEnvVar("JADX_CLS_PROCESS_LIMIT", "50"));

	public static int calcDecompileTimeLimit(int classCount) {
		return classCount * CLS_LIMIT + 5000;
	}

	private final MainWindow mainWindow;
	private final JadxWrapper wrapper;
	private final AtomicInteger complete = new AtomicInteger(0);
	private final AtomicInteger completeIndex = new AtomicInteger(0);
	private int expectedCompleteCount;

	private ProcessResult result;

	private final ExecutorService indexQueue = Executors.newSingleThreadExecutor();

	public DecompileTask(MainWindow mainWindow, JadxWrapper wrapper) {
		this.mainWindow = mainWindow;
		this.wrapper = wrapper;
	}

	@Override
	public String getTitle() {
		return NLS.str("progress.decompile");
	}

	@Override
	public List<Runnable> scheduleJobs() {
		IndexService indexService = mainWindow.getCacheObject().getIndexService();
		List<JavaClass> classes = wrapper.getIncludedClasses();
		expectedCompleteCount = classes.size();

		indexService.setComplete(false);
		complete.set(0);
		completeIndex.set(0);

		List<List<JavaClass>> batches;
		try {
			batches = wrapper.buildDecompileBatches(classes);
		} catch (Exception e) {
			LOG.error("Decompile batches build error", e);
			return Collections.emptyList();
		}
		ICodeCache codeCache = wrapper.getArgs().getCodeCache();
		List<Runnable> jobs = new ArrayList<>(batches.size());
		for (List<JavaClass> batch : batches) {
			jobs.add(() -> {
				for (JavaClass cls : batch) {
					try {
						if (!codeCache.contains(cls.getRawName())) {
							cls.decompile();
						}
					} catch (Throwable e) {
						LOG.error("Failed to decompile class: {}", cls, e);
					} finally {
						complete.incrementAndGet();
					}
					try {
						addToIndex(cls);
					} catch (Exception e) {
						LOG.error("Failed to index class: {}", cls, e);
					}
				}
			});
		}
		return jobs;
	}

	/**
	 * Schedule indexing in one thread
	 */
	private void addToIndex(JavaClass cls) {
		indexQueue.execute(() -> {
			try {
				IndexService indexService = mainWindow.getCacheObject().getIndexService();
				if (indexService.indexCls(cls)) {
					completeIndex.incrementAndGet();
				} else {
					LOG.debug("Index skipped for {}", cls);
				}
			} catch (Throwable e) {
				LOG.error("Failed to index class: {}", cls, e);
			}
		});
	}

	private void waitIndexToComplete() {
		try {
			long start = System.currentTimeMillis();
			indexQueue.shutdown();
			boolean done = indexQueue.awaitTermination(1, TimeUnit.MINUTES);
			if (LOG.isDebugEnabled()) {
				LOG.debug("Index queue terminated: {} in {} ms",
						done ? "clear" : "timeout", System.currentTimeMillis() - start);
			}
		} catch (Exception e) {
			LOG.warn("Failed to terminate index queue", e);
		}
	}

	@Override
	public void onDone(ITaskInfo taskInfo) {
		waitIndexToComplete(); // TODO: allow to run task on complete

		long taskTime = taskInfo.getTime();
		long avgPerCls = taskTime / Math.max(expectedCompleteCount, 1);
		int timeLimit = timeLimit();
		int skippedCls = expectedCompleteCount - complete.get();
		int skippedIndex = expectedCompleteCount - completeIndex.get();
		if (LOG.isInfoEnabled()) {
			LOG.info("Decompile and index task complete in " + taskTime + " ms (avg " + avgPerCls + " ms per class)"
					+ ", classes: " + expectedCompleteCount
					+ ", skipped: " + skippedCls
					+ ", skipped index: " + skippedIndex
					+ ", time limit:{ total: " + timeLimit + "ms, per cls: " + CLS_LIMIT + "ms }"
					+ ", status: " + taskInfo.getStatus());
		}
		this.result = new ProcessResult(Math.max(skippedCls, skippedIndex), taskInfo.getStatus(), timeLimit);
	}

	@Override
	public boolean canBeCanceled() {
		return true;
	}

	@Override
	public int timeLimit() {
		return calcDecompileTimeLimit(expectedCompleteCount);
	}

	@Override
	public boolean checkMemoryUsage() {
		return true;
	}

	public ProcessResult getResult() {
		return result;
	}
}
