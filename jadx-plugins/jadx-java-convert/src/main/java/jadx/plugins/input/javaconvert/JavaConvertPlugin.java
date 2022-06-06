package jadx.plugins.input.javaconvert;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.input.JadxInputPlugin;
import jadx.api.plugins.input.data.ILoadResult;
import jadx.api.plugins.input.data.impl.EmptyLoadResult;
import jadx.api.plugins.options.JadxPluginOptions;
import jadx.api.plugins.options.OptionDescription;
import jadx.plugins.input.dex.DexInputPlugin;

public class JavaConvertPlugin implements JadxInputPlugin, JadxPluginOptions {

	public static final String PLUGIN_ID = "java-convert";

	private final DexInputPlugin dexInput = new DexInputPlugin();
	private final JavaConvertOptions options = new JavaConvertOptions();
	private final JavaConvertLoader loader = new JavaConvertLoader(options);

	@Override
	public JadxPluginInfo getPluginInfo() {
		return new JadxPluginInfo(
				PLUGIN_ID,
				"JavaConvert",
				"Convert .class, .jar and .aar files to dex",
				"java-input");
	}

	@Override
	public ILoadResult loadFiles(List<Path> input) {
		ConvertResult result = loader.process(input);
		if (result.isEmpty()) {
			result.close();
			return EmptyLoadResult.INSTANCE;
		}
		return dexInput.loadFiles(result.getConverted(), result);
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
