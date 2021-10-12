package jadx.plugins.input.raung;

import java.nio.file.Path;
import java.util.List;

import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.input.JadxInputPlugin;
import jadx.api.plugins.input.data.ILoadResult;
import jadx.api.plugins.input.data.impl.EmptyLoadResult;
import jadx.plugins.input.java.JavaInputPlugin;

public class RaungInputPlugin implements JadxInputPlugin {

	@Override
	public JadxPluginInfo getPluginInfo() {
		return new JadxPluginInfo(
				"raung-input",
				"RaungInput",
				"Load .raung files");
	}

	@Override
	public ILoadResult loadFiles(List<Path> input) {
		RaungConvert convert = new RaungConvert();
		if (!convert.execute(input)) {
			return EmptyLoadResult.INSTANCE;
		}
		return JavaInputPlugin.loadClassFiles(convert.getFiles(), convert);
	}
}
