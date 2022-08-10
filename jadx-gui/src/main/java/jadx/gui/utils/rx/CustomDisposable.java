package jadx.gui.utils.rx;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.disposables.Disposable;

public class CustomDisposable implements Disposable {

	private final AtomicBoolean disposed = new AtomicBoolean(false);
	private final Runnable disposeTask;

	public CustomDisposable(Runnable disposeTask) {
		this.disposeTask = disposeTask;
	}

	@Override
	public void dispose() {
		try {
			disposeTask.run();
		} finally {
			disposed.set(true);
		}
	}

	@Override
	public boolean isDisposed() {
		return disposed.get();
	}
}
