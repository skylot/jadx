package jadx.api.plugins.loader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import jadx.api.plugins.JadxPlugin;

/**
 * Loading plugins from current classpath
 */
public class JadxBasePluginLoader implements JadxPluginLoader {

	@Override
	public List<JadxPlugin> load() {
		List<JadxPlugin> list = new ArrayList<>();
		ServiceLoader<JadxPlugin> plugins = ServiceLoader.load(JadxPlugin.class);
		for (JadxPlugin plugin : plugins) {
			list.add(plugin);
		}
		return list;
	}

	@Override
	public void close() throws IOException {
		// nothing to close
	}
}
