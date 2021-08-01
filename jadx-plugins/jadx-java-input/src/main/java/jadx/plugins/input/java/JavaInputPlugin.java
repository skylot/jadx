package jadx.plugins.input.java;

import java.nio.file.Path;
import java.util.List;

import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.input.JadxInputPlugin;
import jadx.api.plugins.input.data.ILoadResult;
import jadx.api.plugins.input.data.impl.EmptyLoadResult;

public class JavaInputPlugin implements JadxInputPlugin {

	public static final JadxPluginInfo PLUGIN_INFO = new JadxPluginInfo(
			"java-input",
			"JavaInput",
			"Load .class and .jar files");

	@Override
	public JadxPluginInfo getPluginInfo() {
		return PLUGIN_INFO;
	}

	@Override
	public ILoadResult loadFiles(List<Path> inputFiles) {
		List<JavaClassReader> readers = JavaFileLoader.collectFiles(inputFiles);
		if (readers.isEmpty()) {
			return EmptyLoadResult.INSTANCE;
		}
		return new JavaLoadResult(readers);
	}
}
