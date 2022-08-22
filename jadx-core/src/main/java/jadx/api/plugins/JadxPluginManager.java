package jadx.api.plugins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.impl.plugins.PluginsContext;
import jadx.api.plugins.options.JadxPluginOptions;
import jadx.api.plugins.options.OptionDescription;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class JadxPluginManager {
	private static final Logger LOG = LoggerFactory.getLogger(JadxPluginManager.class);

	private final Set<PluginData> allPlugins = new TreeSet<>();
	private final Map<String, String> provideSuggestions = new TreeMap<>();

	private List<JadxPlugin> resolvedPlugins = Collections.emptyList();

	public JadxPluginManager() {
	}

	/**
	 * Add suggestion how to resolve conflicting plugins
	 */
	public void providesSuggestion(String provides, String pluginId) {
		provideSuggestions.put(provides, pluginId);
	}

	public void load() {
		allPlugins.clear();
		ServiceLoader<JadxPlugin> jadxPlugins = ServiceLoader.load(JadxPlugin.class);
		for (JadxPlugin plugin : jadxPlugins) {
			addPlugin(plugin);
		}
		resolve();
	}

	public void register(JadxPlugin plugin) {
		Objects.requireNonNull(plugin);
		PluginData addedPlugin = addPlugin(plugin);
		LOG.debug("Register plugin: {}", addedPlugin.getPluginId());
		resolve();
	}

	private PluginData addPlugin(JadxPlugin plugin) {
		PluginData pluginData = new PluginData(plugin, plugin.getPluginInfo());
		LOG.debug("Loading plugin: {}", pluginData.getPluginId());
		if (!allPlugins.add(pluginData)) {
			throw new IllegalArgumentException("Duplicate plugin id: " + pluginData + ", class " + plugin.getClass());
		}
		return pluginData;
	}

	public boolean unload(String pluginId) {
		boolean result = allPlugins.removeIf(pd -> {
			String id = pd.getPluginId();
			boolean match = id.equals(pluginId);
			if (match) {
				LOG.debug("Unload plugin: {}", id);
			}
			return match;
		});
		resolve();
		return result;
	}

	public List<JadxPlugin> getAllPlugins() {
		if (allPlugins.isEmpty()) {
			load();
		}
		return allPlugins.stream().map(PluginData::getPlugin).collect(Collectors.toList());
	}

	public List<JadxPlugin> getResolvedPlugins() {
		return Collections.unmodifiableList(resolvedPlugins);
	}

	private synchronized void resolve() {
		Map<String, List<PluginData>> provides = allPlugins.stream()
				.collect(Collectors.groupingBy(p -> p.getInfo().getProvides()));
		List<PluginData> result = new ArrayList<>(provides.size());
		provides.forEach((provide, list) -> {
			if (list.size() == 1) {
				result.add(list.get(0));
			} else {
				String suggestion = provideSuggestions.get(provide);
				if (suggestion != null) {
					list.stream().filter(p -> p.getPluginId().equals(suggestion))
							.findFirst()
							.ifPresent(result::add);
				} else {
					PluginData selected = list.get(0);
					result.add(selected);
					LOG.debug("Select providing '{}' plugin '{}', candidates: {}", provide, selected, list);
				}
			}
		});
		Collections.sort(result);
		resolvedPlugins = result.stream().map(PluginData::getPlugin).collect(Collectors.toList());
	}

	public void initAll(PluginsContext context) {
		init(context, getAllPlugins());
	}

	public void initResolved(PluginsContext context) {
		init(context, resolvedPlugins);
	}

	private void init(PluginsContext context, List<JadxPlugin> plugins) {
		for (JadxPlugin plugin : plugins) {
			try {
				context.setCurrentPlugin(plugin);
				plugin.init(context);
			} catch (Exception e) {
				String pluginId = plugin.getPluginInfo().getPluginId();
				throw new JadxRuntimeException("Failed to init plugin: " + pluginId, e);
			}
		}
		for (Map.Entry<JadxPlugin, JadxPluginOptions> entry : context.getOptionsMap().entrySet()) {
			verifyOptions(entry.getKey(), entry.getValue());
		}
	}

	private void verifyOptions(JadxPlugin plugin, JadxPluginOptions options) {
		String pluginId = plugin.getPluginInfo().getPluginId();
		List<OptionDescription> descriptions = options.getOptionsDescriptions();
		if (descriptions == null) {
			throw new IllegalArgumentException("Null option descriptions in plugin id: " + pluginId);
		}
		String prefix = pluginId + '.';
		descriptions.forEach(descObj -> {
			String optName = descObj.name();
			if (optName == null || !optName.startsWith(prefix)) {
				throw new IllegalArgumentException("Plugin option name should start with plugin id: '" + prefix + "', option: " + optName);
			}
			String desc = descObj.description();
			if (desc == null || desc.isEmpty()) {
				throw new IllegalArgumentException("Plugin option description not set, plugin: " + pluginId);
			}
			List<String> values = descObj.values();
			if (values == null) {
				throw new IllegalArgumentException("Plugin option values is null, option: " + optName + ", plugin: " + pluginId);
			}
		});
	}

	private static final class PluginData implements Comparable<PluginData> {
		private final JadxPlugin plugin;
		private final JadxPluginInfo info;

		private PluginData(JadxPlugin plugin, JadxPluginInfo info) {
			this.plugin = plugin;
			this.info = info;
		}

		public JadxPlugin getPlugin() {
			return plugin;
		}

		public JadxPluginInfo getInfo() {
			return info;
		}

		public String getPluginId() {
			return info.getPluginId();
		}

		@Override
		public int compareTo(@NotNull JadxPluginManager.PluginData o) {
			return this.info.getPluginId().compareTo(o.info.getPluginId());
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof PluginData)) {
				return false;
			}
			PluginData that = (PluginData) o;
			return getInfo().getPluginId().equals(that.getInfo().getPluginId());
		}

		@Override
		public int hashCode() {
			return info.getPluginId().hashCode();
		}

		@Override
		public String toString() {
			return info.getPluginId();
		}
	}
}
