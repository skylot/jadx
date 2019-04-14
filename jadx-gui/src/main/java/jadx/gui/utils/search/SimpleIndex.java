package jadx.gui.utils.search;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;

public class SimpleIndex<T> implements SearchIndex<T> {

	private final List<String> keys = new ArrayList<>();
	private final List<T> values = new ArrayList<>();

	private final Object syncData = new Object();

	@Override
	public void put(String str, T value) {
		synchronized (syncData) {
			keys.add(str);
			values.add(value);
		}
	}

	@Override
	public void put(StringRef str, T value) {
		throw new UnsupportedOperationException("StringRef not supported");
	}

	@Override
	public boolean isStringRefSupported() {
		return false;
	}

	private boolean isMatched(String str, String searchStr, boolean caseInsensitive) {
		if (caseInsensitive) {
			return StringUtils.containsIgnoreCase(str, searchStr);
		} else {
			return str.contains(searchStr);
		}
	}

	@Override
	public Flowable<T> search(final String searchStr, final boolean caseInsensitive) {
		return Flowable.create(emitter -> {
			synchronized (syncData) {
				int size = keys.size();
				for (int i = 0; i < size; i++) {
					if (isMatched(keys.get(i), searchStr, caseInsensitive)) {
						emitter.onNext(values.get(i));
					}
					if (emitter.isCancelled()) {
						return;
					}
				}
			}
			emitter.onComplete();
		}, BackpressureStrategy.LATEST);
	}

	@Override
	public int size() {
		synchronized (syncData) {
			return keys.size();
		}
	}
}
