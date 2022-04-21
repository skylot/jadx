package jadx.plugins.input.dex;

import java.io.Closeable;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.input.JadxInputPlugin;
import jadx.api.plugins.input.data.ILoadResult;
import jadx.api.plugins.input.data.impl.EmptyLoadResult;
import jadx.api.plugins.options.JadxPluginOptions;
import jadx.api.plugins.options.OptionDescription;
import jadx.api.plugins.utils.CommonFileUtils;

public class DexInputPlugin implements JadxInputPlugin, JadxPluginOptions {
	public static final String PLUGIN_ID = "dex-input";

	private final DexInputOptions options = new DexInputOptions();
	private final DexFileLoader loader = new DexFileLoader(options);

	@Override
	public JadxPluginInfo getPluginInfo() {
		return new JadxPluginInfo(PLUGIN_ID, "DexInput", "Load .dex and .apk files");
	}

	@Override
	public ILoadResult loadFiles(List<Path> input) {
		return loadFiles(input, null);
	}

	public ILoadResult loadFiles(List<Path> inputFiles, @Nullable Closeable closeable) {
		List<DexReader> dexReaders = loader.collectDexFiles(inputFiles);
		if (dexReaders.isEmpty()) {
			return EmptyLoadResult.INSTANCE;
		}
		return new DexLoadResult(dexReaders, closeable);
	}

	public ILoadResult loadDex(byte[] content, @Nullable String fileName) {
		String fileLabel = fileName == null ? "input.dex" : fileName;
		DexReader dexReader = loader.loadDexReader(fileLabel, content);
		return new DexLoadResult(Collections.singletonList(dexReader), null);
	}

	public ILoadResult loadDexFromInputStream(InputStream in, @Nullable String fileLabel) {
		try {
			return loadDex(CommonFileUtils.loadBytes(in), fileLabel);
		} catch (Exception e) {
			throw new DexException("Failed to read input stream", e);
		}
	}

	@Override
	public void setOptions(Map<String, String> options) {
		this.options.apply(options);
	}

	@Override
	public List<OptionDescription> getOptionsDescriptions() {
		return this.options.buildOptionsDescriptions();
	}
}
