package jadx.gui.utils.fileswatcher;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.processors.PublishProcessor;

import jadx.gui.ui.MainWindow;
import jadx.gui.utils.UiUtils;

public class LiveReloadWorker {
	private static final Logger LOG = LoggerFactory.getLogger(LiveReloadWorker.class);

	private final MainWindow mainWindow;
	private final PublishProcessor<Path> processor;
	private volatile boolean started = false;
	private ExecutorService executor;
	private FilesWatcher watcher;

	@SuppressWarnings("ResultOfMethodCallIgnored")
	public LiveReloadWorker(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
		this.processor = PublishProcessor.create();
		this.processor
				.debounce(1, TimeUnit.SECONDS)
				.subscribe(path -> {
					LOG.debug("Reload triggered");
					UiUtils.uiRun(mainWindow::reopen);
				});
	}

	public boolean isStarted() {
		return started;
	}

	public synchronized void updateState(boolean enabled) {
		if (this.started == enabled) {
			return;
		}
		if (enabled) {
			LOG.debug("Starting live reload worker");
			start();
		} else {
			LOG.debug("Stopping live reload worker");
			stop();
		}
	}

	private void onUpdate(Path path, WatchEvent.Kind<Path> pathKind) {
		LOG.debug("Path updated: {}", path);
		processor.onNext(path);
	}

	private synchronized void start() {
		try {
			watcher = new FilesWatcher(mainWindow.getProject().getFilePaths(), this::onUpdate);
			executor = Executors.newSingleThreadExecutor();
			started = true;
			executor.submit(watcher::watch);
		} catch (Exception e) {
			LOG.warn("Failed to start live reload worker", e);
			resetState();
		}
	}

	private synchronized void stop() {
		try {
			watcher.cancel();
			executor.shutdownNow();
			boolean canceled = executor.awaitTermination(5, TimeUnit.SECONDS);
			if (!canceled) {
				LOG.warn("Failed to cancel live reload worker");
			}
		} catch (Exception e) {
			LOG.warn("Failed to stop live reload worker", e);
		} finally {
			resetState();
		}
	}

	private void resetState() {
		started = false;
		executor = null;
		watcher = null;
	}
}
