package jadx.gui.jobs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ICodeCache;
import jadx.api.JavaClass;
import jadx.gui.JadxWrapper;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

public class DecompileTask extends CancelableBackgroundTask {
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

	public DecompileTask(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
		this.wrapper = mainWindow.getWrapper();
	}

	@Override
	public String getTitle() {
		return NLS.str("progress.decompile");
	}

	@Override
	public List<Runnable> scheduleJobs() {
		if (mainWindow.getCacheObject().isFullDecompilationFinished()) {
			return Collections.emptyList();
		}

		List<JavaClass> classes = wrapper.getIncludedClasses();
		expectedCompleteCount = classes.size();
		complete.set(0);

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
					if (isCanceled()) {
						return;
					}
					try {
						if (!codeCache.contains(cls.getRawName())) {
							cls.decompile();
						}
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
			LOG.info("Decompile and index task complete in " + taskTime + " ms (avg " + avgPerCls + " ms per class)"
					+ ", classes: " + expectedCompleteCount
					+ ", skipped: " + skippedCls
					+ ", time limit:{ total: " + timeLimit + "ms, per cls: " + CLS_LIMIT + "ms }"
					+ ", status: " + taskInfo.getStatus());
		}
		result = new ProcessResult(skippedCls, taskInfo.getStatus(), timeLimit);

		wrapper.unloadClasses();
		processDecompilationResults();
		System.gc();

		mainWindow.getCacheObject().setFullDecompilationFinished(skippedCls == 0);
	}

	private void processDecompilationResults() {
		int skippedCls = result.getSkipped();
		if (skippedCls == 0) {
			return;
		}
		TaskStatus status = result.getStatus();
		LOG.warn("Decompile and indexing of some classes skipped: {}, status: {}", skippedCls, status);
		switch (status) {
			case CANCEL_BY_USER: {
				String reason = NLS.str("message.userCancelTask");
				String message = NLS.str("message.indexIncomplete", reason, skippedCls);
				JOptionPane.showMessageDialog(mainWindow, message);
				break;
			}
			case CANCEL_BY_TIMEOUT: {
				String reason = NLS.str("message.taskTimeout", result.getTimeLimit());
				String message = NLS.str("message.indexIncomplete", reason, skippedCls);
				JOptionPane.showMessageDialog(mainWindow, message);
				break;
			}
			case CANCEL_BY_MEMORY: {
				mainWindow.showHeapUsageBar();
				JOptionPane.showMessageDialog(mainWindow, NLS.str("message.indexingClassesSkipped", skippedCls));
				break;
			}
		}
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
