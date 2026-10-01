package jadx.core.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.loader.JadxPluginLoader;
import jadx.api.plugins.options.JadxPluginOptions;
import jadx.api.plugins.options.OptionDescription;
import jadx.core.plugins.versions.VerifyRequiredVersion;

public class JadxPluginManager {
	private static final Logger LOG = LoggerFactory.getLogger(JadxPluginManager.class);

	private final JadxArgs jadxArgs;
	private final Set<String> disabledPlugins;
	private final JadxPluginsData pluginsData;
	private final SortedSet<PluginRuntime> allPlugins = new TreeSet<>();
	private final SortedSet<PluginRuntime> resolvedPlugins = new TreeSet<>();
	private final Map<String, String> provideSuggestions = new TreeMap<>();

	private final List<Consumer<PluginRuntime>> addPluginListeners = new ArrayList<>();

	public JadxPluginManager(JadxArgs args) {
		this.jadxArgs = args;
		this.disabledPlugins = args.getDisabledPlugins();
		this.pluginsData = new JadxPluginsData(this);
	}

	/**
	 * Add suggestion how to resolve conflicting plugins
	 */
	public void providesSuggestion(String provides, String pluginId) {
		provideSuggestions.put(provides, pluginId);
	}

	public void load(JadxPluginLoader pluginLoader) {
		List<JadxPlugin> plugins = pluginLoader.load();

		// allow repeated load (used in passes reload) but keep plugins added by 'register' method
		Set<String> loadedIds = plugins.stream()
				.map(p -> p.getPluginInfo().getPluginId())
				.collect(Collectors.toSet());
		allPlugins.removeIf(context -> loadedIds.contains(context.getPluginId()));

		VerifyRequiredVersion verifyRequiredVersion = new VerifyRequiredVersion();
		for (JadxPlugin plugin : plugins) {
			addPlugin(plugin, verifyRequiredVersion);
		}
		resolve();
	}

	public @Nullable PluginRuntime register(JadxPlugin plugin) {
		Objects.requireNonNull(plugin);
		PluginRuntime addedPlugin = addPlugin(plugin, new VerifyRequiredVersion());
		if (addedPlugin == null) {
			LOG.debug("Plugin not registered: {}", plugin.getPluginInfo().getPluginId());
			return null;
		}
		LOG.debug("Register plugin: {}", addedPlugin.getPluginId());
		resolve();
		return addedPlugin;
	}

	private @Nullable PluginRuntime addPlugin(JadxPlugin plugin, VerifyRequiredVersion verifyRequiredVersion) {
		PluginRuntime pluginRuntime = new PluginRuntime(plugin, jadxArgs);
		if (disabledPlugins.contains(pluginRuntime.getPluginId())) {
			return null;
		}
		String requiredJadxVersion = pluginRuntime.getPluginInfo().getRequiredJadxVersion();
		if (!verifyRequiredVersion.isCompatible(requiredJadxVersion)) {
			LOG.warn("Plugin '{}' not loaded: requires '{}' jadx version which it is not compatible with current: {}",
					pluginRuntime, requiredJadxVersion, verifyRequiredVersion.getJadxVersion());
			return null;
		}
		LOG.debug("Loading plugin: {}", pluginRuntime);
		if (!allPlugins.add(pluginRuntime)) {
			throw new IllegalArgumentException("Duplicate plugin id: " + pluginRuntime + ", class " + plugin.getClass());
		}
		addPluginListeners.forEach(l -> l.accept(pluginRuntime));
		return pluginRuntime;
	}

	public boolean unload(String pluginId) {
		boolean result = allPlugins.removeIf(context -> {
			if (context.getPluginId().equals(pluginId)) {
				LOG.debug("Unload plugin: {}", pluginId);
				return true;
			}
			return false;
		});
		resolve();
		return result;
	}

	public SortedSet<PluginRuntime> getAllPlugins() {
		return allPlugins;
	}

	public SortedSet<PluginRuntime> getResolvedPlugins() {
		return resolvedPlugins;
	}

	private synchronized void resolve() {
		Map<String, List<PluginRuntime>> provides = allPlugins.stream()
				.collect(Collectors.groupingBy(p -> p.getPluginInfo().getProvides()));
		List<PluginRuntime> resolved = new ArrayList<>(provides.size());
		provides.forEach((provide, list) -> {
			if (list.size() == 1) {
				resolved.add(list.get(0));
			} else {
				String suggestion = provideSuggestions.get(provide);
				if (suggestion != null) {
					list.stream().filter(p -> p.getPluginId().equals(suggestion))
							.findFirst()
							.ifPresent(resolved::add);
				} else {
					PluginRuntime selected = list.get(0);
					resolved.add(selected);
					LOG.debug("Select providing '{}' plugin '{}', candidates: {}", provide, selected, list);
				}
			}
		});
		resolvedPlugins.clear();
		resolvedPlugins.addAll(resolved);
	}

	public void initAll(JadxDecompiler decompiler) {
		init(decompiler, allPlugins);
	}

	public void initResolved(JadxDecompiler decompiler) {
		init(decompiler, resolvedPlugins);
	}

	public void init(JadxDecompiler decompiler, SortedSet<PluginRuntime> plugins) {
		AppContext defAppContext = buildDefaultAppContext();
		for (PluginRuntime pluginRuntime : plugins) {
			try {
				if (pluginRuntime.getAppContext() == null) {
					pluginRuntime.setAppContext(defAppContext);
				}
				pluginRuntime.init(new PluginContext(decompiler, pluginsData, pluginRuntime));
			} catch (Exception e) {
				LOG.error("Failed to init plugin: {}", pluginRuntime.getPluginId(), e);
			}
		}
		for (PluginRuntime pluginRuntime : plugins) {
			PluginContext context = pluginRuntime.getPluginContext();
			if (context != null) {
				JadxPluginOptions options = context.getOptions();
				if (options != null) {
					verifyOptions(context, options);
				}
			}
		}
	}

	public void unloadAll() {
		unload(allPlugins);
	}

	public void unloadResolved() {
		unload(resolvedPlugins);
	}

	public void unload(SortedSet<PluginRuntime> pluginContexts) {
		for (PluginRuntime context : pluginContexts) {
			try {
				context.unload();
			} catch (Exception e) {
				LOG.warn("Failed to unload plugin: {}", context.getPluginId(), e);
			}
		}
	}

	private AppContext buildDefaultAppContext() {
		AppContext appContext = new AppContext();
		appContext.setGuiContext(null);
		appContext.setFilesGetter(jadxArgs.getFilesGetter());
		return appContext;
	}

	private void verifyOptions(PluginContext pluginContext, JadxPluginOptions options) {
		String pluginId = pluginContext.getPluginId();
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

	public void registerAddPluginListener(Consumer<PluginRuntime> listener) {
		this.addPluginListeners.add(listener);
		// run for already added plugins
		getAllPlugins().forEach(listener);
	}
}
