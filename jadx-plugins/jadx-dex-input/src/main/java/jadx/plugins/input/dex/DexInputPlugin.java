package jadx.plugins.input.dex;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.List;

import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.input.JadxInputPlugin;
import jadx.api.plugins.input.data.ILoadResult;
import jadx.api.plugins.input.data.impl.EmptyLoadResult;

public class DexInputPlugin implements JadxInputPlugin {

	public DexInputPlugin() {
		DexFileLoader.resetDexUniqId();
	}

	@Override
	public JadxPluginInfo getPluginInfo() {
		return new JadxPluginInfo("dex-input", "DexInput", "Load .dex and .apk files");
	}

	@Override
	public ILoadResult loadFiles(List<Path> input) {
		return loadDexFiles(input, null);
	}

	public static ILoadResult loadDexFiles(List<Path> inputFiles, Closeable closeable) {
		List<DexReader> dexReaders = DexFileLoader.collectDexFiles(inputFiles);
		if (dexReaders.isEmpty()) {
			return EmptyLoadResult.INSTANCE;
		}
		return new DexLoadResult(dexReaders, closeable);
	}
}
