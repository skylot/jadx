package jadx.plugins.input.java;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.List;

import org.jetbrains.annotations.Nullable;

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
		return loadClassFiles(inputFiles, null);
	}

	public static ILoadResult loadClassFiles(List<Path> inputFiles, @Nullable Closeable closeable) {
		List<JavaClassReader> readers = new JavaFileLoader().collectFiles(inputFiles);
		if (readers.isEmpty()) {
			return EmptyLoadResult.INSTANCE;
		}
		return new JavaLoadResult(readers, closeable);
	}
}
