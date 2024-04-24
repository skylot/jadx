package jadx.plugins.input.smali;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.input.data.impl.EmptyCodeLoader;
import jadx.plugins.input.dex.DexInputPlugin;

public class SmaliInputPlugin implements JadxPlugin {
	public static final String PLUGIN_ID = "smali-input";

	private final SmaliInputOptions options = new SmaliInputOptions();

	@Override
	public JadxPluginInfo getPluginInfo() {
		return new JadxPluginInfo(PLUGIN_ID, "Smali Input", "Load .smali files");
	}

	@Override
	public void init(JadxPluginContext context) {
		context.registerOptions(options);
		options.setThreads(context.getArgs().getThreadsCount());

		DexInputPlugin dexInput = context.plugins().getInstance(DexInputPlugin.class);
		context.addCodeInput(input -> {
			SmaliConvert convert = new SmaliConvert();
			if (!convert.execute(input, options)) {
				return EmptyCodeLoader.INSTANCE;
			}
			return dexInput.loadDexData(convert.getDexData());
		});
	}
}
