package jadx.gui.jobs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

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
		List<JavaClass> classes = wrapper.getIncludedClasses();
		expectedCompleteCount = classes.size();

		IndexService indexService = mainWindow.getCacheObject().getIndexService();
		indexService.setComplete(false);
		complete.set(0);

		List<Runnable> jobs = new ArrayList<>(expectedCompleteCount + 1);
		for (JavaClass cls : classes) {
			jobs.add(() -> {
				cls.decompile();
				indexService.indexCls(cls);
				complete.incrementAndGet();
			});
		}
		jobs.add(indexService::indexResources);
		startTime = System.currentTimeMillis();
		return jobs;
	}

	@Override
	public void onFinish(TaskStatus status) {
		long taskTime = System.currentTimeMillis() - startTime;
		long avgPerCls = taskTime / expectedCompleteCount;
		LOG.info("Decompile task complete in {} ms (avg {} ms per class), classes: {},"
				+ " time limit:{ total: {}ms, per cls: {}ms }, status: {}",
				taskTime, avgPerCls, expectedCompleteCount, timeLimit(), CLS_LIMIT, status);

		IndexService indexService = mainWindow.getCacheObject().getIndexService();
		indexService.setComplete(true);

		int complete = this.complete.get();
		int skipped = expectedCompleteCount - complete;
		if (skipped == 0) {
			return;
		}
		LOG.warn("Decompile and indexing of some classes skipped: {}, status: {}", skipped, status);
		switch (status) {
			case CANCEL_BY_USER: {
				String reason = NLS.str("message.userCancelTask");
				String message = NLS.str("message.indexIncomplete", reason, skipped);
				JOptionPane.showMessageDialog(mainWindow, message);
				break;
			}
			case CANCEL_BY_TIMEOUT: {
				String reason = NLS.str("message.taskTimeout", timeLimit());
				String message = NLS.str("message.indexIncomplete", reason, skipped);
				JOptionPane.showMessageDialog(mainWindow, message);
				break;
			}
			case CANCEL_BY_MEMORY: {
				mainWindow.showHeapUsageBar();
				JOptionPane.showMessageDialog(mainWindow, NLS.str("message.indexingClassesSkipped", skipped));
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
		return expectedCompleteCount * CLS_LIMIT + 5000;
	}

	@Override
	public boolean checkMemoryUsage() {
		return true;
	}
}
