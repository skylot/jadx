package jadx.gui.utils.dbg;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.utils.UiUtils;

/**
 * Watch for UI thread state, if it stuck log a warning with stacktrace
 */
public class UIWatchDog {
	private static final Logger LOG = LoggerFactory.getLogger(UIWatchDog.class);

	private static final boolean RUN_ON_START = false;

	private static final int UI_MAX_DELAY_MS = 1000;
	private static final int CHECK_INTERVAL_MS = 100;

	public static void onStart() {
		if (RUN_ON_START) {
			UiUtils.uiRun(UIWatchDog::toggle);
		}
	}

	public static synchronized void toggle() {
		if (SwingUtilities.isEventDispatchThread()) {
			INSTANCE.toggleState(Thread.currentThread());
		} else {
			throw new JadxRuntimeException("This method should be called in UI thread");
		}
	}

	private static final UIWatchDog INSTANCE = new UIWatchDog();

	private final AtomicBoolean enabled = new AtomicBoolean(false);
	private final ExecutorService executor = Executors.newSingleThreadExecutor();
	private Future<?> taskFuture;

	private UIWatchDog() {
		// singleton
	}

	private void toggleState(Thread uiThread) {
		if (enabled.get()) {
			// stop
			enabled.set(false);
			if (taskFuture != null) {
				try {
					taskFuture.get(CHECK_INTERVAL_MS * 5, TimeUnit.MILLISECONDS);
				} catch (Throwable e) {
					LOG.warn("Stopping UI watchdog error", e);
				}
			}
		} else {
			// start
			enabled.set(true);
			taskFuture = executor.submit(() -> start(uiThread));
		}
	}

	@SuppressWarnings("BusyWait")
	private void start(Thread uiThread) {
		LOG.debug("UI watchdog started");
		try {
			Exception e = new JadxRuntimeException("at");
			TimeMeasure tm = new TimeMeasure();
			boolean stuck = false;
			long reportTime = 0;
			while (enabled.get()) {
				if (uiThread.getState() == Thread.State.TIMED_WAITING) {
					if (!stuck) {
						tm.start();
						stuck = true;
						reportTime = UI_MAX_DELAY_MS;
					} else {
						tm.end();
						long time = tm.getTime();
						if (time > reportTime) {
							e.setStackTrace(uiThread.getStackTrace());
							LOG.warn("UI events thread stuck for {}ms", time, e);
							reportTime += UI_MAX_DELAY_MS;
						}
					}
				} else {
					stuck = false;
				}
				Thread.sleep(CHECK_INTERVAL_MS);
			}
		} catch (Throwable e) {
			LOG.error("UI watchdog fail", e);
		}
		LOG.debug("UI watchdog stopped");
	}

	private static final class TimeMeasure {
		private long start;
		private long end;

		public void start() {
			start = System.currentTimeMillis();
		}

		public void end() {
			end = System.currentTimeMillis();
		}

		public long getTime() {
			return end - start;
		}
	}
}
