package jadx.gui.cache.usage;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.api.usage.IUsageInfoCache;
import jadx.api.usage.IUsageInfoData;
import jadx.api.usage.impl.InMemoryUsageInfoCache;
import jadx.core.dex.nodes.RootNode;

public class UsageInfoCache implements IUsageInfoCache {

	private static final Object LOAD_DATA_SYNC = new Object();

	private final Path usageFile;
	private final List<File> inputs;
	private final InMemoryUsageInfoCache memCache = new InMemoryUsageInfoCache();
	private @Nullable RawUsageData rawUsageData;

	public UsageInfoCache(Path cacheDir, List<File> inputFiles) {
		usageFile = cacheDir.resolve("usage");
		inputs = inputFiles;
	}

	@Override
	public @Nullable IUsageInfoData get(RootNode root) {
		IUsageInfoData memData = memCache.get(root);
		if (memData != null) {
			return memData;
		}
		synchronized (LOAD_DATA_SYNC) {
			if (rawUsageData == null) {
				rawUsageData = UsageFileAdapter.load(usageFile, inputs);
			}
			if (rawUsageData != null) {
				UsageData data = new UsageData(root, rawUsageData);
				memCache.set(root, data);
				return data;
			}
		}
		return null;
	}

	@Override
	public void set(RootNode root, IUsageInfoData data) {
		memCache.set(root, data);
		UsageFileAdapter.save(data, usageFile, inputs);
	}

	@Override
	public void close() {
		rawUsageData = null;
		memCache.close();
	}
}
