package jadx.gui.jobs;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class CancelableBackgroundTask implements IBackgroundTask {

	private final AtomicBoolean cancel = new AtomicBoolean(false);

	@Override
	public boolean isCanceled() {
		return cancel.get();
	}

	@Override
	public void cancel() {
		cancel.set(true);
	}

	public void resetCancel() {
		cancel.set(false);
	}

	@Override
	public boolean canBeCanceled() {
		return true;
	}
}
