package jadx.plugins.input.smali;

import java.nio.file.Path;
import java.util.List;

import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.input.JadxInputPlugin;
import jadx.api.plugins.input.data.ILoadResult;
import jadx.api.plugins.input.data.impl.EmptyLoadResult;
import jadx.plugins.input.dex.DexInputPlugin;

public class SmaliInputPlugin implements JadxInputPlugin {

	@Override
	public JadxPluginInfo getPluginInfo() {
		return new JadxPluginInfo("smali-input", "SmaliInput", "Load .smali files");
	}

	@Override
	public ILoadResult loadFiles(List<Path> input) {
		SmaliConvert convert = new SmaliConvert();
		if (!convert.execute(input)) {
			return EmptyLoadResult.INSTANCE;
		}
		return DexInputPlugin.loadDexFiles(convert.getDexFiles(), convert);
	}
}
