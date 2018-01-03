package jadx.gui.jobs;

import javax.swing.*;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.ui.ProgressPanel;
import jadx.gui.utils.CacheObject;
import jadx.gui.utils.Utils;
import jadx.gui.utils.search.TextSearchIndex;

public class BackgroundWorker extends SwingWorker<Void, Void> {
	private static final Logger LOG = LoggerFactory.getLogger(BackgroundWorker.class);

	private final CacheObject cache;
	private final ProgressPanel progressPane;

	public BackgroundWorker(CacheObject cacheObject, ProgressPanel progressPane) {
		this.cache = cacheObject;
		this.progressPane = progressPane;
	}

	public void exec() {
		if (isDone()) {
			return;
		}
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				progressPane.setVisible(true);
			}
		});
		addPropertyChangeListener(progressPane);
		execute();
	}

	public void stop() {
		if (isDone()) {
			return;
		}
		LOG.debug("Canceling background jobs ...");
		cancel(false);
	}

	@Override
	protected Void doInBackground() throws Exception {
		try {
			System.gc();
			LOG.debug("Memory usage: Before decompile: {}", Utils.memoryInfo());
			runJob(cache.getDecompileJob());

			LOG.debug("Memory usage: Before index: {}", Utils.memoryInfo());
			runJob(cache.getIndexJob());
			LOG.debug("Memory usage: After index: {}", Utils.memoryInfo());

			System.gc();
			LOG.debug("Memory usage: After gc: {}", Utils.memoryInfo());

			TextSearchIndex searchIndex = cache.getTextIndex();
			if (searchIndex != null && searchIndex.getSkippedCount() > 0) {
				LOG.warn("Indexing of some classes skipped, count: {}, low memory: {}",
						searchIndex.getSkippedCount(), Utils.memoryInfo());
			}
		} catch (Exception e) {
			LOG.error("Exception in background worker", e);
		}
		return null;
	}

	private void runJob(BackgroundJob job) {
		if (isCancelled()) {
			return;
		}
		progressPane.changeLabel(this, job.getInfoString());
		Future<Boolean> future = job.process();
		while (!future.isDone()) {
			try {
				setProgress(job.getProgress());
				if (isCancelled()) {
					future.cancel(false);
				}
				Thread.sleep(500);
			} catch (Exception e) {
				LOG.error("Background worker error", e);
			}
		}
	}

	@Override
	protected void done() {
		progressPane.setVisible(false);
	}

}
