package jadx.plugins.input.xapk;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.JadxPluginInfoBuilder;

public class XApkInputPlugin implements JadxPlugin {

	private XApkLoader loader;

	@Override
	public JadxPluginInfo getPluginInfo() {
		return JadxPluginInfoBuilder.pluginId("xapk-input")
				.name("XApk Input")
				.description("Load .xapk files")
				.build();
	}

	@Override
	public void init(JadxPluginContext context) {
		loader = new XApkLoader(context);
		XApkCustomInput customInput = new XApkCustomInput(context, loader);
		context.addCodeInput(customInput);
		context.getDecompiler().addCustomResourcesLoader(customInput);
	}

	@Override
	public void unload() {
		if (loader != null) {
			loader.unload();
		}
	}
}
