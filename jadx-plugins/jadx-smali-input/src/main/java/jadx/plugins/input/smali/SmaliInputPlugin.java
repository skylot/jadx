package jadx.plugins.input.smali;

import java.nio.file.Path;
import java.util.List;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.input.ICodeLoader;
import jadx.api.plugins.input.JadxCodeInput;
import jadx.api.plugins.input.data.impl.EmptyCodeLoader;
import jadx.plugins.input.dex.DexInputPlugin;

public class SmaliInputPlugin implements JadxPlugin, JadxCodeInput {

	private final DexInputPlugin dexInput = new DexInputPlugin();

	@Override
	public JadxPluginInfo getPluginInfo() {
		return new JadxPluginInfo("smali-input", "SmaliInput", "Load .smali files");
	}

	@Override
	public void init(JadxPluginContext context) {
		context.addCodeInput(this);
	}

	@Override
	public ICodeLoader loadFiles(List<Path> input) {
		SmaliConvert convert = new SmaliConvert();
		if (!convert.execute(input)) {
			return EmptyCodeLoader.INSTANCE;
		}
		return dexInput.loadFiles(convert.getDexFiles(), convert);
	}
}
