package jadx.gui.jobs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

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
	private long startTime;

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
		List<JavaClass> classesForIndex = wrapper.getIncludedClasses()
				.stream()
				.filter(indexService::isIndexNeeded)
				.collect(Collectors.toList());
		expectedCompleteCount = classesForIndex.size();

		indexService.setComplete(false);
		complete.set(0);

		List<Runnable> jobs = new ArrayList<>(expectedCompleteCount + 1);
		jobs.add(indexService::indexResources);
		for (List<JavaClass> batch : wrapper.buildDecompileBatches(classesForIndex)) {
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
		jobs.add(() -> {
			for (JavaClass cls : classesForIndex) {
				try {
					// TODO: a lot of synchronizations to index object, not effective for parallel usage
					indexService.indexCls(cls);
				} catch (Throwable e) {
					LOG.error("Failed to index class: {}", cls, e);
				}
			}
		});
		startTime = System.currentTimeMillis();
		return jobs;
	}

	@Override
	public void onFinish(TaskStatus status, long skippedJobs) {
		long taskTime = System.currentTimeMillis() - startTime;
		long avgPerCls = taskTime / Math.max(expectedCompleteCount, 1);
		if (LOG.isInfoEnabled()) {
			LOG.info("Decompile task complete in " + taskTime + " ms (avg " + avgPerCls + " ms per class)"
					+ ", classes: " + expectedCompleteCount
					+ ", time limit:{ total: " + timeLimit() + "ms, per cls: " + CLS_LIMIT + "ms }"
					+ ", status: " + status);
		}

		IndexService indexService = mainWindow.getCacheObject().getIndexService();
		indexService.setComplete(true);
		if (skippedJobs == 0) {
			return;
		}

		int skippedCls = expectedCompleteCount - complete.get();
		LOG.warn("Decompile and indexing of some classes skipped: {}, status: {}", skippedCls, status);
		switch (status) {
			case CANCEL_BY_USER: {
				String reason = NLS.str("message.userCancelTask");
				String message = NLS.str("message.indexIncomplete", reason, skippedCls);
				JOptionPane.showMessageDialog(mainWindow, message);
				break;
			}
			case CANCEL_BY_TIMEOUT: {
				String reason = NLS.str("message.taskTimeout", timeLimit());
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
}
