package jadx.gui.utils.rx;

import java.util.concurrent.TimeUnit;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.disposables.Disposable;

public class DebounceUpdate {

	private FlowableEmitter<Boolean> emitter;
	private final Disposable disposable;

	public DebounceUpdate(int timeMs, Runnable action) {
		FlowableOnSubscribe<Boolean> source = emitter -> this.emitter = emitter;
		disposable = Flowable.create(source, BackpressureStrategy.LATEST)
				.debounce(timeMs, TimeUnit.MILLISECONDS)
				.subscribe(v -> action.run());
	}

	public void requestUpdate() {
		emitter.onNext(Boolean.TRUE);
	}

	public void dispose() {
		disposable.dispose();
	}
}
