package jadx.plugins.input.javaconvert;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.input.JadxInputPlugin;
import jadx.api.plugins.input.data.ILoadResult;
import jadx.api.plugins.input.data.impl.EmptyLoadResult;
import jadx.plugins.input.dex.DexFileLoader;
import jadx.plugins.input.dex.DexLoadResult;
import jadx.plugins.input.dex.DexReader;

public class JavaConvertPlugin implements JadxInputPlugin {

	@Override
	public JadxPluginInfo getPluginInfo() {
		return new JadxPluginInfo("java-convert", "JavaConvert", "Convert .jar and .class files to dex");
	}

	@Override
	public ILoadResult loadFiles(List<Path> input) {
		ConvertResult result = JavaConvertLoader.process(input);
		if (result.isEmpty()) {
			result.deleteTemp();
			return EmptyLoadResult.INSTANCE;
		}
		List<DexReader> dexReaders = DexFileLoader.collectDexFiles(result.getConverted());
		return new DexLoadResult(dexReaders) {
			@Override
			public void close() throws IOException {
				super.close();
				result.deleteTemp();
			}
		};
	}
}
