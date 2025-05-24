package jadx.gui.utils.rx;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableEmitter;
import io.reactivex.rxjava3.core.FlowableOnSubscribe;
import io.reactivex.rxjava3.disposables.Disposable;

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
