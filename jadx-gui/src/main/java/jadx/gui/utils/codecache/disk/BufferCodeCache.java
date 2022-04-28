package jadx.gui.utils.codecache.disk;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import jadx.api.ICodeCache;
import jadx.api.ICodeInfo;
import jadx.gui.utils.UiUtils;

/**
 * Allow fast access to recent items
 */
public class BufferCodeCache implements ICodeCache {
	private static final Logger LOG = LoggerFactory.getLogger(BufferCodeCache.class);

	private final LoadingCache<String, ICodeInfo> memCache;
	private final ICodeCache backCache;

	public BufferCodeCache(ICodeCache backCache) {
		this.backCache = backCache;
		this.memCache = CacheBuilder.newBuilder()
				.maximumSize(100)
				.expireAfterWrite(5, TimeUnit.MINUTES)
				.expireAfterAccess(5, TimeUnit.MINUTES)
				.recordStats()
				.build(new CodeInfoCacheLoader());

		UiUtils.debugTimer(30, () -> LOG.debug("Buffer cache size: {}, stats: {}", memCache.size(), memCache.stats()));
	}

	private final class CodeInfoCacheLoader extends CacheLoader<String, ICodeInfo> {
		@Override
		public ICodeInfo load(String key) {
			return backCache.get(key);
		}
	}

	@Override
	@NotNull
	public ICodeInfo get(String clsFullName) {
		try {
			return this.memCache.get(clsFullName);
		} catch (Exception e) {
			LOG.error("Cache error", e);
			return ICodeInfo.EMPTY;
		}
	}

	@Override
	public void add(String clsFullName, ICodeInfo codeInfo) {
		this.memCache.put(clsFullName, codeInfo);
		this.backCache.add(clsFullName, codeInfo);
	}

	@Override
	public void remove(String clsFullName) {
		this.memCache.invalidate(clsFullName);
		this.backCache.remove(clsFullName);
	}

	@Override
	public void close() throws IOException {
		this.memCache.cleanUp();
		this.backCache.close();
	}
}
