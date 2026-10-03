package jadx.gui.plugins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.loader.JadxPluginLoader;
import jadx.core.plugins.PluginRuntime;

/**
 * Plugins loader for project: already loaded global plugins and plugins from project loader
 */
public class GuiPluginsLoader implements JadxPluginLoader {
	private static final Logger LOG = LoggerFactory.getLogger(GuiPluginsLoader.class);

	private final List<JadxPlugin> globalPlugins;
	private final JadxPluginLoader projectPluginsLoader;

	public GuiPluginsLoader(Collection<PluginRuntime> globalPlugins, JadxPluginLoader projectPluginsLoader) {
		this.globalPlugins = globalPlugins.stream()
				.map(PluginRuntime::getPluginInstance)
				.collect(Collectors.toList());
		this.projectPluginsLoader = projectPluginsLoader;
	}

	@Override
	public List<JadxPlugin> load() {
		Set<String> globalPluginIds = globalPlugins.stream()
				.map(p -> p.getPluginInfo().getPluginId())
				.collect(Collectors.toSet());
		List<JadxPlugin> list = new ArrayList<>(globalPlugins);
		for (JadxPlugin plugin : projectPluginsLoader.load()) {
			String pluginId = plugin.getPluginInfo().getPluginId();
			if (globalPluginIds.contains(pluginId)) {
				LOG.warn("Plugin '{}' not loaded: global plugin with same id already loaded, class: {}",
						pluginId, plugin.getClass().getName());
			} else {
				list.add(plugin);
			}
		}
		return list;
	}

	@Override
	public void close() throws IOException {
		projectPluginsLoader.close();
	}
}
