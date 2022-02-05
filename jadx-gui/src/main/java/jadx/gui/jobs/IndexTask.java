package jadx.gui.jobs;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JavaClass;
import jadx.gui.JadxWrapper;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;

public class IndexTask implements IBackgroundTask {
	private static final Logger LOG = LoggerFactory.getLogger(IndexTask.class);

	private final MainWindow mainWindow;
	private final JadxWrapper wrapper;
	private final AtomicInteger complete = new AtomicInteger(0);
	private int expectedCompleteCount;

	private ProcessResult result;

	public IndexTask(MainWindow mainWindow, JadxWrapper wrapper) {
		this.mainWindow = mainWindow;
		this.wrapper = wrapper;
	}

	@Override
	public String getTitle() {
		return NLS.str("progress.index");
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

		List<Runnable> jobs = new ArrayList<>(2);
		jobs.add(indexService::indexResources);
		jobs.add(() -> {
			for (JavaClass cls : classesForIndex) {
				try {
					// TODO: a lot of synchronizations to index object, not efficient for parallel usage
					if (indexService.indexCls(cls)) {
						complete.incrementAndGet();
					} else {
						LOG.debug("Index skipped for {}", cls);
					}
				} catch (Throwable e) {
					LOG.error("Failed to index class: {}", cls, e);
				}
			}
		});
		return jobs;
	}

	@Override
	public void onDone(ITaskInfo taskInfo) {
		int skippedCls = expectedCompleteCount - complete.get();
		if (LOG.isInfoEnabled()) {
			LOG.info("Index task complete in " + taskInfo.getTime() + " ms"
					+ ", classes: " + expectedCompleteCount
					+ ", skipped: " + skippedCls
					+ ", status: " + taskInfo.getStatus());
		}
		IndexService indexService = mainWindow.getCacheObject().getIndexService();
		indexService.setComplete(true);
		this.result = new ProcessResult(skippedCls, taskInfo.getStatus(), 0);
	}

	@Override
	public boolean canBeCanceled() {
		return true;
	}

	@Override
	public boolean checkMemoryUsage() {
		return true;
	}

	public ProcessResult getResult() {
		return result;
	}
}
