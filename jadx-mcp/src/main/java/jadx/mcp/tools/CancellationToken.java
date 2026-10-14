package jadx.mcp.tools;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jetbrains.annotations.Nullable;

/**
 * Cooperative cancellation flag for one tool call.
 * Worker threads are never interrupted (interrupts can close NIO channels used by jadx caches),
 * so long-running operations must call {@link #throwIfCancelled()} periodically.
 */
public final class CancellationToken {

	public enum Reason {
		CLIENT_CANCELLED,
		TIMEOUT,
		PROJECT_CLOSED,
		SESSION_CLOSED,
		SERVER_STOPPED
	}

	private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
	private volatile @Nullable Reason reason;

	public boolean isCancelled() {
		return reason != null;
	}

	public @Nullable Reason getReason() {
		return reason;
	}

	public void cancel(Reason cancelReason) {
		synchronized (this) {
			if (reason != null) {
				return;
			}
			reason = cancelReason;
		}
		for (Runnable listener : listeners) {
			try {
				listener.run();
			} catch (Exception e) {
				// ignore listener errors
			}
		}
	}

	/**
	 * Listener called once on cancel (immediately if already cancelled)
	 */
	public void onCancel(Runnable listener) {
		listeners.add(listener);
		if (reason != null) {
			listener.run();
		}
	}

	public void throwIfCancelled() {
		Reason r = reason;
		if (r != null) {
			throw new ToolCancelledException(r);
		}
	}
}
