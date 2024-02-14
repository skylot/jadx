package jadx.plugins.input.raung;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.data.JadxPluginRuntimeData;
import jadx.api.plugins.input.data.impl.EmptyCodeLoader;

public class RaungInputPlugin implements JadxPlugin {

	@Override
	public JadxPluginInfo getPluginInfo() {
		return new JadxPluginInfo("raung-input", "Raung Input", "Load .raung files");
	}

	@Override
	public void init(JadxPluginContext context) {
		JadxPluginRuntimeData javaInput = context.plugins().getProviding("java-input");
		context.addCodeInput(inputs -> {
			RaungConvert convert = new RaungConvert();
			if (!convert.execute(inputs)) {
				return EmptyCodeLoader.INSTANCE;
			}
			return javaInput.loadCodeFiles(convert.getFiles(), convert);
		});
	}
}
