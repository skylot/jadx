package jadx.gui.plugins;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxArgs;
import jadx.api.gui.plugins.JadxGlobalGuiPlugin;
import jadx.api.gui.plugins.JadxGuiContextExt;
import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.events.types.ReloadSettingsWindow;
import jadx.api.plugins.gui.JadxGuiContext;
import jadx.api.plugins.loader.JadxPluginLoader;
import jadx.api.plugins.options.JadxPluginOptions;
import jadx.cli.JadxAppCommon;
import jadx.cli.plugins.JadxFilesGetter;
import jadx.core.plugins.AppContext;
import jadx.core.plugins.JadxPluginManager;
import jadx.core.plugins.PluginRuntime;
import jadx.gui.plugins.context.CommonGuiPluginsContext;
import jadx.gui.settings.JadxSettings;
import jadx.gui.ui.MainWindow;
import jadx.plugins.tools.JadxExternalPluginsLoader;
import jadx.plugins.tools.JadxPluginsTools;
import jadx.plugins.tools.data.JadxPluginMetadata;

public class GuiPluginsManager {
	private static final Logger LOG = LoggerFactory.getLogger(GuiPluginsManager.class);

	private static final long SLOW_GLOBAL_INIT_MS = 1000;

	private final MainWindow mainWindow;
	private final JadxPluginsTools pluginsTools;
	private final JadxArgs globalArgs;
	private final JadxPluginManager globalPluginManager;
	private final CommonGuiPluginsContext guiPluginsContext;
	private final JadxExternalPluginsLoader globalPluginsLoader;
	private final Set<String> scheduledUnload = Collections.synchronizedSet(new LinkedHashSet<>());
	private final Set<String> scheduledLoad = Collections.synchronizedSet(new LinkedHashSet<>());

	public GuiPluginsManager(MainWindow mainWindow) {
		this(mainWindow, JadxPluginsTools.getInstance());
	}

	GuiPluginsManager(MainWindow mainWindow, JadxPluginsTools pluginsTools) {
		this.mainWindow = mainWindow;
		this.pluginsTools = pluginsTools;
		this.globalPluginsLoader = new JadxExternalPluginsLoader(
				JadxGlobalGuiPlugin.class::isAssignableFrom, pluginsTools);
		this.guiPluginsContext = new CommonGuiPluginsContext(mainWindow);
		this.globalArgs = buildJadxArgs(mainWindow.getSettings());
		this.globalPluginManager = new JadxPluginManager(globalArgs);
	}

	private static JadxArgs buildJadxArgs(JadxSettings settings) {
		JadxArgs jadxArgs = settings.toJadxArgs();
		jadxArgs.setFilesGetter(JadxFilesGetter.INSTANCE);
		JadxAppCommon.applyEnvVars(jadxArgs);
		return jadxArgs;
	}

	public void load() {
		try {
			long start = System.currentTimeMillis();

			initGuiPluginsContextForGlobalScope();
			globalPluginManager.load(globalPluginsLoader);
			SortedSet<PluginRuntime> globalPlugins = globalPluginManager.getResolvedPlugins();
			runGlobalInit(globalPlugins);
			if (!globalPlugins.isEmpty()) {
				// settings window can be opened before global plugins init
				mainWindow.events().send(ReloadSettingsWindow.INSTANCE);
			}

			if (LOG.isDebugEnabled()) {
				LOG.debug("Initialized {} global gui plugins in {} ms",
						globalPlugins.size(), System.currentTimeMillis() - start);
			}
		} catch (Exception e) {
			LOG.error("Failed to load gui plugins", e);
		}
	}

	public JadxPluginLoader buildProjectPluginLoader() {
		JadxExternalPluginsLoader loader = new JadxExternalPluginsLoader(
				cls -> !JadxGlobalGuiPlugin.class.isAssignableFrom(cls), pluginsTools);
		List<JadxPlugin> globalPlugins = new ArrayList<>();
		for (PluginRuntime pluginRuntime : getGlobalPlugins()) {
			globalPlugins.add(pluginRuntime.getPluginInstance());
		}
		loader.addExternalPlugins(globalPlugins);
		return loader;
	}

	/**
	 * Loader for project decompiler: global plugins and project plugins
	 */
	public JadxPluginLoader buildGuiPluginsLoader() {
		return new GuiPluginsLoader(getGlobalPlugins(), buildProjectPluginLoader());
	}

	public void initGuiPluginsContextForGlobalScope() {
		initGuiPluginsContext(globalPluginManager, globalArgs, true);
	}

	public void initGuiPluginsContext(JadxPluginManager pluginManager, JadxArgs jadxArgs, boolean isGlobalPlugin) {
		pluginManager.registerAddPluginListener(pluginRuntime -> {
			AppContext appContext = new AppContext();
			appContext.setGuiContext(guiPluginsContext.buildForPlugin(pluginRuntime, isGlobalPlugin));
			appContext.setFilesGetter(jadxArgs.getFilesGetter());
			pluginRuntime.setAppContext(appContext);
			if (!isGlobalPlugin) {
				copyGlobalPluginData(pluginRuntime);
			}
		});
	}

	/**
	 * Transfer plugin context data from global plugin into project
	 */
	private void copyGlobalPluginData(PluginRuntime projectPlugin) {
		for (PluginRuntime globalPlugin : getGlobalPlugins()) {
			if (globalPlugin.getPluginInstance() == projectPlugin.getPluginInstance()) {
				// copy options and gui data
				projectPlugin.registerOptions(globalPlugin.getOptions());
				guiPluginsContext.copyGlobalPluginData(globalPlugin, projectPlugin);
				return;
			}
		}
	}

	public SortedSet<PluginRuntime> getGlobalPlugins() {
		return globalPluginManager.getResolvedPlugins();
	}

	public void resetProjectScope() {
		guiPluginsContext.resetProjectScope();
	}

	void runGlobalInit(SortedSet<PluginRuntime> globalPlugins) {
		List<PluginRuntime> failedPlugins = new ArrayList<>();
		for (PluginRuntime pluginRuntime : globalPlugins) {
			long start = System.currentTimeMillis();
			try {
				JadxGlobalGuiPlugin plugin = (JadxGlobalGuiPlugin) pluginRuntime.getPluginInstance();
				PluginRuntime.classLoaderWrap(plugin.getClass().getClassLoader(), () -> {
					AppContext appContext = pluginRuntime.getAppContext();
					if (appContext != null) {
						JadxGuiContext guiContext = Objects.requireNonNull(appContext.getGuiContext());
						plugin.globalInit((JadxGuiContextExt) guiContext);
					}
				});
			} catch (Throwable e) {
				LOG.warn("Failed to init global gui plugin: {}", pluginRuntime.getPluginId(), e);
				failedPlugins.add(pluginRuntime);
			}
			long time = System.currentTimeMillis() - start;
			if (time > SLOW_GLOBAL_INIT_MS) {
				LOG.warn("Slow global init of plugin '{}': {} ms, project loading waits for it", pluginRuntime.getPluginId(), time);
			} else {
				LOG.debug("Global init of plugin '{}' done in {} ms", pluginRuntime.getPluginId(), time);
			}
		}
		// don't inject failed plugins into projects and remove added gui entries
		for (PluginRuntime failedPlugin : failedPlugins) {
			globalPluginManager.unload(failedPlugin.getPluginId());
			guiPluginsContext.removeGlobalPlugin(failedPlugin);
		}
	}

	public synchronized void runGlobalUnload() {
		try {
			for (PluginRuntime pluginRuntime : getGlobalPlugins()) {
				globalUnload(pluginRuntime);
			}
		} catch (Exception e) {
			LOG.warn("Failed to unload global gui plugins", e);
		}
	}

	public void scheduleGlobalUnload(String pluginId) {
		scheduledUnload.add(pluginId);
	}

	public void scheduleGlobalLoad(String pluginId) {
		scheduledLoad.add(pluginId);
	}

	public synchronized void runScheduledGlobalChanges() {
		Set<String> unloadRequested = drainScheduled(scheduledUnload);
		Set<String> loadRequested = drainScheduled(scheduledLoad);

		boolean unloaded = false;
		if (!unloadRequested.isEmpty()) {
			Map<String, List<String>> loadedDeps = new HashMap<>();
			for (PluginRuntime runtime : globalPluginManager.getAllPlugins()) {
				loadedDeps.put(runtime.getPluginId(), collectRequiredIds(runtime.getPluginInfo()));
			}
			Set<String> toUnload = collectCascadeDependents(unloadRequested, loadedDeps);
			for (String pluginId : toUnload) {
				if (!unloadRequested.contains(pluginId)
						&& areUnloadedDepsReloaded(loadedDeps.get(pluginId), toUnload, loadRequested)) {
					loadRequested.add(pluginId);
				}
			}
			List<String> orderedUnload = sortPluginIdsByDependencies(toUnload, loadedDeps::get);
			Collections.reverse(orderedUnload);
			for (String pluginId : orderedUnload) {
				PluginRuntime pluginRuntime = globalPluginManager.getAllPlugins().stream()
						.filter(p -> p.getPluginId().equals(pluginId))
						.findFirst()
						.orElse(null);
				if (pluginRuntime != null) {
					globalUnload(pluginRuntime);
					globalPluginManager.unload(pluginId);
					guiPluginsContext.removeGlobalPlugin(pluginRuntime);
					globalPluginsLoader.closeClassLoader(pluginRuntime.getPluginInstance());
					LOG.info("Global plugin unloaded: {}", pluginId);
					unloaded = true;
				}
			}
		}
		if (unloaded) {
			// plugin files released after class loader close
			pluginsTools.deleteLeftoverFiles();
		}
		if (!loadRequested.isEmpty()) {
			Map<String, JadxPluginMetadata> enabledById = new HashMap<>();
			Map<String, List<String>> metadataDeps = new HashMap<>();
			for (JadxPluginMetadata metadata : pluginsTools.getEnabledInstalled()) {
				enabledById.put(metadata.getPluginId(), metadata);
				metadataDeps.put(metadata.getPluginId(), collectRequiredIds(metadata));
			}
			Set<String> currentlyLoadedIds = globalPluginManager.getAllPlugins().stream()
					.map(PluginRuntime::getPluginId)
					.collect(Collectors.toSet());
			Set<String> toLoad = new LinkedHashSet<>(loadRequested);
			for (String dependentId : collectCascadeDependents(loadRequested, metadataDeps)) {
				if (!currentlyLoadedIds.contains(dependentId)) {
					toLoad.add(dependentId);
				}
			}
			List<String> orderedLoad = sortPluginIdsByDependencies(toLoad, metadataDeps::get);
			for (String pluginId : orderedLoad) {
				loadGlobalPlugin(pluginId, enabledById.get(pluginId));
			}
		}
	}

	private void loadGlobalPlugin(String pluginId, @Nullable JadxPluginMetadata metadata) {
		if (metadata == null || metadata.isDisabled()) {
			return;
		}
		List<JadxPlugin> plugins;
		try {
			plugins = globalPluginsLoader.loadPluginsFromPath(pluginsTools.getInstalledPluginPath(metadata));
		} catch (Exception e) {
			LOG.warn("Failed to load global gui plugin: {}", pluginId, e);
			return;
		}
		// empty for project plugins
		plugins.forEach(this::addGlobalPlugin);
	}

	private static Set<String> drainScheduled(Set<String> set) {
		synchronized (set) {
			Set<String> drained = new LinkedHashSet<>(set);
			set.clear();
			return drained;
		}
	}

	private static Set<String> collectCascadeDependents(
			Set<String> roots, Map<String, List<String>> depsById) {
		Set<String> result = new LinkedHashSet<>(roots);
		boolean added;
		do {
			added = false;
			for (Map.Entry<String, List<String>> entry : depsById.entrySet()) {
				if (!result.contains(entry.getKey())) {
					for (String reqId : entry.getValue()) {
						if (result.contains(reqId)) {
							result.add(entry.getKey());
							added = true;
							break;
						}
					}
				}
			}
		} while (added);
		return result;
	}

	private static boolean areUnloadedDepsReloaded(
			@Nullable List<String> deps, Set<String> toUnload, Set<String> loadRequested) {
		if (deps == null) {
			return false;
		}
		for (String dep : deps) {
			if (toUnload.contains(dep) && !loadRequested.contains(dep)) {
				return false;
			}
		}
		return true;
	}

	private static List<String> collectRequiredIds(JadxPluginInfo info) {
		return collectRequiredIds(info.getParentPlugin(), info.getDependencyPlugins());
	}

	private static List<String> collectRequiredIds(JadxPluginMetadata metadata) {
		return collectRequiredIds(metadata.getParentPlugin(), metadata.getDependencyPlugins());
	}

	private static List<String> collectRequiredIds(
			@Nullable String parentPlugin, @Nullable Collection<String> dependencyPlugins) {
		List<String> reqs = new ArrayList<>();
		if (parentPlugin != null && !parentPlugin.isBlank()) {
			reqs.add(parentPlugin);
		}
		if (dependencyPlugins != null) {
			for (String dep : dependencyPlugins) {
				if (dep.indexOf(':') == -1 && !reqs.contains(dep)) {
					reqs.add(dep);
				}
			}
		}
		return reqs;
	}

	private static List<String> sortPluginIdsByDependencies(
			Collection<String> ids, Function<String, @Nullable List<String>> depsLookup) {
		Set<String> targetSet = new LinkedHashSet<>(ids);
		List<String> ordered = new ArrayList<>(ids.size());
		Set<String> visiting = new HashSet<>();
		Set<String> visited = new HashSet<>();
		for (String id : ids) {
			visitPluginId(id, targetSet, depsLookup, visiting, visited, ordered);
		}
		return ordered;
	}

	private static void visitPluginId(
			String id,
			Set<String> targetSet,
			Function<String, @Nullable List<String>> depsLookup,
			Set<String> visiting,
			Set<String> visited,
			List<String> ordered) {
		if (!targetSet.contains(id) || visited.contains(id) || !visiting.add(id)) {
			return;
		}
		List<String> deps = depsLookup.apply(id);
		if (deps != null) {
			for (String dep : deps) {
				visitPluginId(dep, targetSet, depsLookup, visiting, visited, ordered);
			}
		}
		visiting.remove(id);
		visited.add(id);
		ordered.add(id);
	}

	void addGlobalPlugin(JadxPlugin plugin) {
		try {
			PluginRuntime pluginRuntime = globalPluginManager.register(plugin);
			if (pluginRuntime != null) {
				runGlobalInit(new TreeSet<>(Collections.singleton(pluginRuntime)));
				if (getGlobalPlugins().contains(pluginRuntime)) {
					LOG.info("Global plugin loaded: {}", pluginRuntime.getPluginId());
					return;
				}
			}
		} catch (Exception e) {
			LOG.warn("Failed to add global gui plugin: {}", plugin.getPluginInfo().getPluginId(), e);
		}
		globalPluginsLoader.closeClassLoader(plugin);
	}

	private void globalUnload(PluginRuntime pluginRuntime) {
		try {
			JadxGlobalGuiPlugin plugin = (JadxGlobalGuiPlugin) pluginRuntime.getPluginInstance();
			PluginRuntime.classLoaderWrap(plugin.getClass().getClassLoader(), plugin::globalUnload);
		} catch (Exception e) {
			LOG.warn("Failed to unload global gui plugin: {}", pluginRuntime.getPluginId(), e);
		}
	}

	public void updateGlobalPluginsOptions() {
		Map<String, String> pluginOptions = mainWindow.getSettings().getPluginOptions();
		globalArgs.setPluginOptions(pluginOptions);
		for (PluginRuntime pluginRuntime : getGlobalPlugins()) {
			JadxPluginOptions options = pluginRuntime.getOptions();
			if (options != null) {
				try {
					pluginRuntime.classLoaderWrap(() -> options.setOptions(pluginOptions));
				} catch (Exception e) {
					LOG.warn("Failed to update options of global gui plugin: {}", pluginRuntime.getPluginId(), e);
				}
			}
		}
	}

	public CommonGuiPluginsContext getPluginsContext() {
		return guiPluginsContext;
	}

	public JadxPluginManager getGlobalPluginManager() {
		return globalPluginManager;
	}
}
