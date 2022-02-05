package jadx.gui.jobs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	private int expectedCompleteCount;

	private ProcessResult result;

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

		List<List<JavaClass>> batches;
		try {
			batches = wrapper.buildDecompileBatches(classes);
		} catch (Exception e) {
			LOG.error("Decompile batches build error", e);
			return Collections.emptyList();
		}
		List<Runnable> jobs = new ArrayList<>(batches.size());
		for (List<JavaClass> batch : batches) {
			jobs.add(() -> {
				for (JavaClass cls : batch) {
					try {
						cls.decompile();
					} catch (Throwable e) {
						LOG.error("Failed to decompile class: {}", cls, e);
					} finally {
						complete.incrementAndGet();
					}
				}
			});
		}
		return jobs;
	}

	@Override
	public void onDone(ITaskInfo taskInfo) {
		long taskTime = taskInfo.getTime();
		long avgPerCls = taskTime / Math.max(expectedCompleteCount, 1);
		int timeLimit = timeLimit();
		int skippedCls = expectedCompleteCount - complete.get();
		if (LOG.isInfoEnabled()) {
			LOG.info("Decompile task complete in " + taskTime + " ms (avg " + avgPerCls + " ms per class)"
					+ ", classes: " + expectedCompleteCount
					+ ", skipped: " + skippedCls
					+ ", time limit:{ total: " + timeLimit + "ms, per cls: " + CLS_LIMIT + "ms }"
					+ ", status: " + taskInfo.getStatus());
		}
		this.result = new ProcessResult(skippedCls, taskInfo.getStatus(), timeLimit);
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
