package jadx.plugins.input.dex;

import java.nio.file.Path;
import java.util.List;

import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.input.JadxInputPlugin;
import jadx.api.plugins.input.data.ILoadResult;

public class DexInputPlugin implements JadxInputPlugin {

	@Override
	public JadxPluginInfo getPluginInfo() {
		return new JadxPluginInfo("dex-input", "DexInput", "Load .dex and .apk files");
	}

	@Override
	public ILoadResult loadFiles(List<Path> input) {
		return new DexLoadResult(DexFileLoader.collectDexFiles(input));
	}
}
