package jadx.gui.plugins;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.loader.JadxPluginLoader;

public class TestPluginsLoader implements JadxPluginLoader {
	private final List<JadxPlugin> plugins;

	public TestPluginsLoader(JadxPlugin... plugins) {
		this(Arrays.asList(plugins));
	}

	public TestPluginsLoader(List<JadxPlugin> plugins) {
		this.plugins = plugins;
	}

	@Override
	public List<JadxPlugin> load() {
		return new ArrayList<>(plugins);
	}

	@Override
	public void close() {
	}
}
