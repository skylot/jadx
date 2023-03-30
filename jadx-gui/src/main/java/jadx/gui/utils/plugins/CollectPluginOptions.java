package jadx.gui.utils.plugins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginManager;
import jadx.api.plugins.options.JadxPluginOptions;
import jadx.gui.JadxWrapper;

/**
 * Collect options from all plugins.
 * Init not yet loaded plugins in new temporary context.
 * Support case if decompiler in wrapper not initialized yet.
 */
public class CollectPluginOptions {

	private final JadxWrapper wrapper;
	private final Map<Class<?>, PluginWithOptions> plugins;

	public CollectPluginOptions(JadxWrapper wrapper) {
		this.wrapper = wrapper;
		this.plugins = new HashMap<>();
	}

	public List<PluginWithOptions> build() {
		wrapper.getCurrentDecompiler().ifPresent(decompiler -> {
			List<JadxPlugin> loadedPlugins = decompiler.getPluginManager().getResolvedPlugins();
			addOptions(decompiler, loadedPlugins);
		});
		// collect and init not loaded plugins in new context
		try (JadxDecompiler decompiler = new JadxDecompiler(new JadxArgs())) {
			JadxPluginManager pluginManager = decompiler.getPluginManager();
			List<JadxPlugin> missingPlugins = new ArrayList<>();
			for (JadxPlugin plugin : pluginManager.getAllPlugins()) {
				if (!plugins.containsKey(plugin.getClass())) {
					missingPlugins.add(plugin);
				}
			}
			pluginManager.init(decompiler.getPluginsContext(), missingPlugins);
			addOptions(decompiler, missingPlugins);
		}
		return plugins.values().stream()
				.filter(data -> data != PluginWithOptions.NULL)
				.sorted()
				.collect(Collectors.toList());
	}

	private void addOptions(JadxDecompiler decompiler, List<JadxPlugin> loadedPlugins) {
		Map<JadxPlugin, JadxPluginOptions> optionsMap = decompiler.getPluginsContext().getOptionsMap();
		for (JadxPlugin loadedPlugin : loadedPlugins) {
			JadxPluginOptions pluginOptions = optionsMap.get(loadedPlugin);
			PluginWithOptions options;
			if (pluginOptions != null) {
				options = new PluginWithOptions(loadedPlugin, pluginOptions);
			} else {
				options = PluginWithOptions.NULL;
			}
			plugins.put(loadedPlugin.getClass(), options);
		}
	}
}
