package jadx.gui.utils.search;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;

import jadx.gui.utils.UiUtils;

public class CodeIndex<T> implements SearchIndex<T> {

	private static final Logger LOG = LoggerFactory.getLogger(CodeIndex.class);

	private final List<StringRef> keys = new ArrayList<>();
	private final List<T> values = new ArrayList<>();

	@Override
	public void put(String str, T value) {
		throw new UnsupportedOperationException("CodeIndex.put for string not supported");
	}

	@Override
	public synchronized void put(StringRef str, T value) {
		if (str == null || str.length() == 0) {
			return;
		}
		keys.add(str);
		values.add(value);
	}

	@Override
	public boolean isStringRefSupported() {
		return true;
	}

	private boolean isMatched(StringRef key, String str, boolean caseInsensitive) {
		return key.indexOf(str, caseInsensitive) != -1;
	}

	@Override
	public Flowable<T> search(final String searchStr, final boolean caseInsensitive) {
		return Flowable.create(emitter -> {
			int size = size();
			LOG.debug("Code search started: {} ...", searchStr);
			for (int i = 0; i < size; i++) {
				if (isMatched(keys.get(i), searchStr, caseInsensitive)) {
					emitter.onNext(values.get(i));
				}
				if (emitter.isCancelled()) {
					LOG.debug("Code search canceled: {}", searchStr);
					return;
				}
			}
			LOG.debug("Code search complete: {}, memory usage: {}", searchStr, UiUtils.memoryInfo());
			emitter.onComplete();
		}, BackpressureStrategy.LATEST);
	}

	@Override
	public int size() {
		return keys.size();
	}
}
