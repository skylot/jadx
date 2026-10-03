package jadx.gui.plugins;

import java.util.Arrays;
import java.util.List;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.loader.JadxPluginLoader;

public class TestPluginLoader implements JadxPluginLoader {
	private final List<JadxPlugin> plugins;

	public TestPluginLoader(JadxPlugin... plugins) {
		this.plugins = Arrays.asList(plugins);
	}

	@Override
	public List<JadxPlugin> load() {
		return plugins;
	}

	@Override
	public void close() {
	}
}
