package jadx.plugins.input.javaconvert;

import java.nio.file.Path;
import java.util.List;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.input.ICodeLoader;
import jadx.api.plugins.input.JadxCodeInput;
import jadx.api.plugins.input.data.impl.EmptyCodeLoader;
import jadx.plugins.input.dex.DexInputPlugin;

public class JavaConvertPlugin implements JadxPlugin, JadxCodeInput {

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
	public void init(JadxPluginContext context) {
		context.registerOptions(options);
		context.addCodeInput(this);
		context.registerInputsHashSupplier(options::getOptionsHash);
	}

	@Override
	public ICodeLoader loadFiles(List<Path> input) {
		ConvertResult result = loader.process(input);
		if (result.isEmpty()) {
			result.close();
			return EmptyCodeLoader.INSTANCE;
		}
		return dexInput.loadFiles(result.getConverted(), result);
	}
}
