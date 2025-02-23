package jadx.plugins.input.dex;

import java.io.Closeable;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.input.ICodeLoader;
import jadx.api.plugins.input.data.impl.EmptyCodeLoader;
import jadx.api.plugins.utils.CommonFileUtils;
import jadx.plugins.input.dex.utils.IDexData;

public class DexInputPlugin implements JadxPlugin {
	public static final String PLUGIN_ID = "dex-input";

	private final DexInputOptions options = new DexInputOptions();
	private final DexFileLoader loader = new DexFileLoader(options);

	@Override
	public JadxPluginInfo getPluginInfo() {
		return new JadxPluginInfo(PLUGIN_ID, "Dex Input", "Load .dex and .apk files");
	}

	@Override
	public void init(JadxPluginContext context) {
		context.registerOptions(options);
		context.addCodeInput(this::loadFiles);
		loader.setZipReader(context.getZipReader());
	}

	public ICodeLoader loadFiles(List<Path> input) {
		return loadFiles(input, null);
	}

	public ICodeLoader loadFiles(List<Path> inputFiles, @Nullable Closeable closeable) {
		List<DexReader> dexReaders = loader.collectDexFiles(inputFiles);
		if (dexReaders.isEmpty()) {
			return EmptyCodeLoader.INSTANCE;
		}
		return new DexLoadResult(dexReaders, closeable);
	}

	public ICodeLoader loadDex(byte[] content, @Nullable String fileName) {
		String fileLabel = fileName == null ? "input.dex" : fileName;
		List<DexReader> dexReaders = loader.loadDexReaders(fileLabel, content);
		return new DexLoadResult(dexReaders, null);
	}

	public ICodeLoader loadDexFromInputStream(InputStream in, @Nullable String fileLabel) {
		try {
			return loadDex(CommonFileUtils.loadBytes(in), fileLabel);
		} catch (Exception e) {
			throw new DexException("Failed to read input stream", e);
		}
	}

	public ICodeLoader loadDexData(List<IDexData> list) {
		List<DexReader> readers = list.stream()
				.flatMap(data -> loader.loadDexReaders(data.getFileName(), data.getContent()).stream())
				.collect(Collectors.toList());
		return new DexLoadResult(readers, null);
	}
}
