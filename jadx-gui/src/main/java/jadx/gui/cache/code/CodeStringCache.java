package jadx.gui.cache.code;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.reactivestreams.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.disposables.Disposable;
import io.reactivex.processors.PublishProcessor;

import jadx.api.ICodeCache;
import jadx.api.ICodeInfo;
import jadx.api.impl.DelegateCodeCache;
import jadx.gui.utils.UiUtils;

/**
 * Keep code strings for faster search
 */
public class CodeStringCache extends DelegateCodeCache {
	private static final Logger LOG = LoggerFactory.getLogger(CodeStringCache.class);

	private final Map<String, String> codeCache = new ConcurrentHashMap<>();
	private final Subscriber<Boolean> subscriber;
	private final Disposable disposable;

	public CodeStringCache(ICodeCache backCache) {
		super(backCache);
		// reset cache if free memory is low
		// check only on changes (with debounce) to reduce background checks if app not used
		PublishProcessor<Boolean> processor = PublishProcessor.create();
		subscriber = processor;
		disposable = processor.debounce(3, TimeUnit.SECONDS)
				.map(v -> UiUtils.isFreeMemoryAvailable())
				.filter(v -> !v)
				.subscribe(v -> {
					LOG.warn("Free memory is low! Reset code strings cache. Cache size {}", codeCache.size());
					codeCache.clear();
					System.gc();
				});
	}

	@Override
	@Nullable
	public String getCode(String clsFullName) {
		subscriber.onNext(Boolean.TRUE);
		String code = codeCache.get(clsFullName);
		if (code != null) {
			return code;
		}
		String backCode = backCache.getCode(clsFullName);
		if (backCode != null) {
			codeCache.put(clsFullName, backCode);
		}
		return backCode;
	}

	@Override
	public @NotNull ICodeInfo get(String clsFullName) {
		subscriber.onNext(Boolean.TRUE);
		return super.get(clsFullName);
	}

	@Override
	public void add(String clsFullName, ICodeInfo codeInfo) {
		subscriber.onNext(Boolean.TRUE);
		codeCache.put(clsFullName, codeInfo.getCodeStr());
		backCache.add(clsFullName, codeInfo);
	}

	@Override
	public void remove(String clsFullName) {
		codeCache.remove(clsFullName);
		backCache.remove(clsFullName);
	}

	@Override
	public void close() throws IOException {
		try {
			backCache.close();
		} finally {
			codeCache.clear();
			subscriber.onComplete();
			disposable.dispose();
		}
	}
}
