package jadx.plugins.input.javaconvert;

import java.nio.file.Path;
import java.util.List;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.JadxPluginInfoBuilder;
import jadx.api.plugins.data.JadxPluginRuntimeData;
import jadx.api.plugins.input.ICodeLoader;
import jadx.api.plugins.input.JadxCodeInput;
import jadx.api.plugins.input.data.impl.EmptyCodeLoader;
import jadx.plugins.input.dex.DexInputPlugin;

public class JavaConvertPlugin implements JadxPlugin, JadxCodeInput {
	public static final String PLUGIN_ID = "java-convert";

	private final JavaConvertOptions options = new JavaConvertOptions();
	private final JavaConvertLoader loader = new JavaConvertLoader(options);

	private JadxPluginRuntimeData dexInput;

	@Override
	public JadxPluginInfo getPluginInfo() {
		return JadxPluginInfoBuilder.pluginId(PLUGIN_ID)
				.name("Java Convert")
				.description("Convert .class, .jar and .aar files to dex")
				.provides("java-input")
				.build();
	}

	@Override
	public void init(JadxPluginContext context) {
		dexInput = context.plugins().getById(DexInputPlugin.PLUGIN_ID);
		context.registerOptions(options);
		context.addCodeInput(this);
	}

	@Override
	public ICodeLoader loadFiles(List<Path> input) {
		ConvertResult result = loader.process(input);
		if (result.isEmpty()) {
			result.close();
			return EmptyCodeLoader.INSTANCE;
		}
		return dexInput.loadCodeFiles(result.getConverted(), result);
	}
}
