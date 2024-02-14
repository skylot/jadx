package jadx.plugins.input.smali;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.data.JadxPluginRuntimeData;
import jadx.api.plugins.input.data.impl.EmptyCodeLoader;
import jadx.plugins.input.dex.DexInputPlugin;

public class SmaliInputPlugin implements JadxPlugin {

	@Override
	public JadxPluginInfo getPluginInfo() {
		return new JadxPluginInfo("smali-input", "Smali Input", "Load .smali files");
	}

	@Override
	public void init(JadxPluginContext context) {
		JadxPluginRuntimeData dexInput = context.plugins().getById(DexInputPlugin.PLUGIN_ID);
		context.addCodeInput(input -> {
			SmaliConvert convert = new SmaliConvert();
			if (!convert.execute(input)) {
				return EmptyCodeLoader.INSTANCE;
			}
			return dexInput.loadCodeFiles(convert.getDexFiles(), convert);
		});
	}
}
