package jadx.plugins.tools;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.loader.JadxPluginLoader;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.FileUtils;
import jadx.plugins.tools.data.JadxPluginMetadata;

public class JadxExternalPluginsLoader implements JadxPluginLoader {
	private static final Logger LOG = LoggerFactory.getLogger(JadxExternalPluginsLoader.class);

	public static final String JADX_PLUGIN_CLASSLOADER_PREFIX = "jadx-plugin:";

	private final List<URLClassLoader> classLoaders = new ArrayList<>();
	private final Map<String, ClassLoader> loadedClassLoaders = new HashMap<>();
	private final Map<String, ClassLoader> externalParentClassLoaders = new HashMap<>();

	private final Predicate<Class<?>> pluginClassFilter;
	private final JadxPluginsTools pluginsTools;

	public JadxExternalPluginsLoader() {
		this(c -> true);
	}

	public JadxExternalPluginsLoader(Predicate<Class<?>> pluginClassFilter) {
		this(pluginClassFilter, JadxPluginsTools.getInstance());
	}

	public JadxExternalPluginsLoader(Predicate<Class<?>> pluginClassFilter, JadxPluginsTools pluginsTools) {
		this.pluginClassFilter = pluginClassFilter;
		this.pluginsTools = pluginsTools;
	}

	/**
	 * Register externally managed parent plugins (for example GUI global plugins for project plugins).
	 */
	public void addExternalPlugins(Collection<? extends JadxPlugin> plugins) {
		for (JadxPlugin plugin : plugins) {
			JadxPluginInfo info = plugin.getPluginInfo();
			if (info != null) {
				externalParentClassLoaders.put(info.getPluginId(), plugin.getClass().getClassLoader());
			}
		}
	}

	@Override
	public List<JadxPlugin> load() {
		close();
		long start = System.currentTimeMillis();
		Map<String, JadxPlugin> map = new HashMap<>();
		loadFromClsLoader(map, thisClassLoader(), true);
		loadInstalledPlugins(map);

		List<JadxPlugin> list = new ArrayList<>(map.size());
		list.addAll(map.values());
		list.sort(Comparator.comparing(p -> p.getClass().getSimpleName()));
		if (LOG.isDebugEnabled()) {
			LOG.debug("Collected {} plugins in {}ms", list.size(), System.currentTimeMillis() - start);
		}
		return list;
	}

	public JadxPlugin loadFromPath(Path pluginPath) {
		Map<String, JadxPlugin> map = new HashMap<>();
		loadFromPathWithParent(map, pluginPath, thisClassLoader(), false);
		int loaded = map.size();
		if (loaded > 1) {
			String plugins = map.values().stream().map(p -> p.getPluginInfo().getPluginId()).collect(Collectors.joining(", "));
			throw new JadxRuntimeException("Expect only one plugin per jar: " + pluginPath + ", but found: " + loaded + " - " + plugins);
		}
		JadxPlugin plugin = Utils.first(map.values());
		if (plugin == null) {
			throw new JadxRuntimeException("No plugin found in jar: " + pluginPath);
		}
		return plugin;
	}

	List<JadxPlugin> loadMetadataPluginsFromPath(Path pluginPath) {
		Map<String, JadxPlugin> map = new HashMap<>();
		loadFromPathWithParent(map, pluginPath, thisClassLoader(), false);
		return new ArrayList<>(map.values());
	}

	public List<JadxPlugin> loadPluginsFromPath(Path pluginPath) {
		Map<String, JadxPlugin> map = new HashMap<>();
		List<PendingPluginEntry> pending = new ArrayList<>();
		probeOrLoadFromPath(map, pluginPath, pending);
		if (!pending.isEmpty()) {
			PendingPluginEntry entry = pending.get(0);
			ClassLoader parentClsLoader = findParentClassLoader(entry.parentPluginId);
			if (parentClsLoader == null) {
				if (isAcceptedPluginJar(entry.path)) {
					LOG.warn("Skip plugin from {}: parent plugin '{}' not found or disabled",
							entry.path, entry.parentPluginId);
				}
			} else {
				loadFromPathWithParent(map, entry.path, parentClsLoader, true);
			}
		}
		return new ArrayList<>(map.values());
	}

	public void closeClassLoader(JadxPlugin plugin) {
		ClassLoader pluginClsLoader = plugin.getClass().getClassLoader();
		JadxPluginInfo info = plugin.getPluginInfo();
		if (info != null) {
			loadedClassLoaders.remove(info.getPluginId(), pluginClsLoader);
		}
		Iterator<URLClassLoader> it = classLoaders.iterator();
		while (it.hasNext()) {
			URLClassLoader classLoader = it.next();
			if (classLoader == pluginClsLoader) {
				it.remove();
				try {
					classLoader.close();
				} catch (Exception e) {
					LOG.warn("Failed to close class loader of plugin: {}", plugin.getPluginInfo().getPluginId(), e);
				}
				return;
			}
		}
	}

	private boolean loadFromClsLoader(Map<String, JadxPlugin> map, ClassLoader classLoader, boolean registerLoaded) {
		boolean added = false;
		ServiceLoader<JadxPlugin> serviceLoader = ServiceLoader.load(JadxPlugin.class, classLoader);
		for (ServiceLoader.Provider<JadxPlugin> provider : serviceLoader.stream().collect(Collectors.toList())) {
			Class<? extends JadxPlugin> pluginClass = provider.type();
			String clsName = pluginClass.getName();
			if (!map.containsKey(clsName)
					&& pluginClass.getClassLoader() == classLoader
					&& pluginClassFilter.test(pluginClass)) {
				JadxPlugin plugin = provider.get();
				map.put(clsName, plugin);
				if (registerLoaded) {
					registerPluginClassLoader(plugin, classLoader);
				}
				added = true;
			}
		}
		return added;
	}

	private void loadInstalledPlugins(Map<String, JadxPlugin> map) {
		List<PendingPluginEntry> pending = new ArrayList<>();
		for (JadxPluginMetadata metadata : pluginsTools.getEnabledInstalled()) {
			Path pluginPath = pluginsTools.getInstalledPluginPath(metadata);
			String parentPlugin = metadata.getParentPlugin();
			if (parentPlugin != null && !parentPlugin.isBlank()) {
				pending.add(new PendingPluginEntry(pluginPath, parentPlugin,
						Collections.singleton(metadata.getPluginId())));
			} else {
				probeOrLoadFromPath(map, pluginPath, pending);
			}
		}
		for (Path dropinPath : pluginsTools.getDropinsPaths()) {
			probeOrLoadFromPath(map, dropinPath, pending);
		}
		resolvePendingPlugins(map, pending);
	}

	private void probeOrLoadFromPath(Map<String, JadxPlugin> map, Path pluginPath, List<PendingPluginEntry> pending) {
		Map<String, JadxPlugin> probeMap = new HashMap<>();
		URLClassLoader probeLoader = loadFromPathWithParent(probeMap, pluginPath, thisClassLoader(), false);
		if (probeLoader == null) {
			return;
		}
		String parentPluginId = findRequiredParentPlugin(probeMap.values());
		if (parentPluginId != null) {
			Set<String> pluginIds = collectPluginIds(probeMap.values());
			closeLoader(probeLoader);
			pending.add(new PendingPluginEntry(pluginPath, parentPluginId, pluginIds));
			return;
		}
		boolean kept = false;
		for (Map.Entry<String, JadxPlugin> entry : probeMap.entrySet()) {
			if (!map.containsKey(entry.getKey())) {
				map.put(entry.getKey(), entry.getValue());
				registerPluginClassLoader(entry.getValue(), probeLoader);
				kept = true;
			}
		}
		if (!kept) {
			closeLoader(probeLoader);
		}
	}

	private void resolvePendingPlugins(Map<String, JadxPlugin> map, List<PendingPluginEntry> pending) {
		while (!pending.isEmpty()) {
			boolean progressed = false;
			Iterator<PendingPluginEntry> it = pending.iterator();
			while (it.hasNext()) {
				PendingPluginEntry entry = it.next();
				ClassLoader parentClsLoader = findParentClassLoader(entry.parentPluginId);
				if (parentClsLoader != null) {
					loadFromPathWithParent(map, entry.path, parentClsLoader, true);
					it.remove();
					progressed = true;
				}
			}
			if (!progressed) {
				break;
			}
		}
		if (!pending.isEmpty()) {
			checkParentCycles(pending);
			for (PendingPluginEntry entry : pending) {
				if (isAcceptedPluginJar(entry.path)) {
					LOG.warn("Skip plugin from {}: parent plugin '{}' not found or disabled",
							entry.path, entry.parentPluginId);
				}
			}
		}
	}

	private void checkParentCycles(List<PendingPluginEntry> pending) {
		Map<String, String> parentByPlugin = new HashMap<>();
		for (PendingPluginEntry entry : pending) {
			for (String pluginId : entry.pluginIds) {
				parentByPlugin.put(pluginId, entry.parentPluginId);
			}
		}
		for (String startId : parentByPlugin.keySet()) {
			List<String> chain = new ArrayList<>();
			Set<String> seen = new HashSet<>();
			String current = startId;
			while (current != null) {
				if (!seen.add(current)) {
					int idx = chain.indexOf(current);
					List<String> cycle = idx >= 0 ? new ArrayList<>(chain.subList(idx, chain.size())) : new ArrayList<>(chain);
					cycle.add(current);
					throw new JadxRuntimeException("Cyclic parent plugins detected: " + String.join(" -> ", cycle));
				}
				chain.add(current);
				current = parentByPlugin.get(current);
			}
		}
	}

	private boolean isAcceptedPluginJar(Path pluginPath) {
		try (URLClassLoader probeLoader = new URLClassLoader(
				JADX_PLUGIN_CLASSLOADER_PREFIX + pluginPath.getFileName(),
				resolveUrls(pluginPath),
				thisClassLoader())) {
			ServiceLoader<JadxPlugin> serviceLoader = ServiceLoader.load(JadxPlugin.class, probeLoader);
			for (ServiceLoader.Provider<JadxPlugin> provider : serviceLoader.stream().collect(Collectors.toList())) {
				Class<? extends JadxPlugin> pluginClass = provider.type();
				if (pluginClass.getClassLoader() == probeLoader && pluginClassFilter.test(pluginClass)) {
					return true;
				}
			}
			return false;
		} catch (Exception e) {
			LOG.debug("Failed to inspect plugin from: {}", pluginPath, e);
			return true;
		}
	}

	@Nullable
	private URLClassLoader loadFromPathWithParent(
			Map<String, JadxPlugin> map,
			Path pluginPath,
			ClassLoader parentClassLoader,
			boolean registerLoaded) {
		try {
			URL[] urls = resolveUrls(pluginPath);
			String clsLoaderName = JADX_PLUGIN_CLASSLOADER_PREFIX + pluginPath.getFileName();
			URLClassLoader pluginClsLoader = new URLClassLoader(clsLoaderName, urls, parentClassLoader);
			classLoaders.add(pluginClsLoader);
			if (!loadFromClsLoader(map, pluginClsLoader, registerLoaded)) {
				classLoaders.remove(pluginClsLoader);
				pluginClsLoader.close();
				return null;
			}
			return pluginClsLoader;
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to load plugins from: " + pluginPath, e);
		}
	}

	private URL[] resolveUrls(Path pluginPath) {
		if (Files.isDirectory(pluginPath)) {
			URL[] urls = FileUtils.listFiles(pluginPath, file -> FileUtils.hasExtension(file, ".jar"))
					.stream()
					.map(JadxExternalPluginsLoader::toURL)
					.toArray(URL[]::new);
			if (urls.length == 0) {
				throw new JadxRuntimeException("No jar files found in plugin directory");
			}
			return urls;
		}
		if (Files.isRegularFile(pluginPath)) {
			if (FileUtils.hasExtension(pluginPath, ".jar")) {
				return new URL[] { toURL(pluginPath) };
			}
			throw new JadxRuntimeException("Unexpected plugin file format");
		}
		throw new JadxRuntimeException("Plugin file not found");
	}

	@Nullable
	private ClassLoader findParentClassLoader(String parentPluginId) {
		ClassLoader loader = loadedClassLoaders.get(parentPluginId);
		if (loader != null) {
			return loader;
		}
		return externalParentClassLoaders.get(parentPluginId);
	}

	private void registerPluginClassLoader(JadxPlugin plugin, ClassLoader classLoader) {
		JadxPluginInfo info = plugin.getPluginInfo();
		if (info != null) {
			loadedClassLoaders.put(info.getPluginId(), classLoader);
		}
	}

	@Nullable
	private static String findRequiredParentPlugin(Collection<JadxPlugin> plugins) {
		for (JadxPlugin plugin : plugins) {
			JadxPluginInfo info = plugin.getPluginInfo();
			if (info != null && info.getParentPlugin() != null && !info.getParentPlugin().isBlank()) {
				return info.getParentPlugin();
			}
		}
		return null;
	}

	private static Set<String> collectPluginIds(Collection<JadxPlugin> plugins) {
		Set<String> ids = new HashSet<>(plugins.size());
		for (JadxPlugin plugin : plugins) {
			JadxPluginInfo info = plugin.getPluginInfo();
			if (info != null) {
				ids.add(info.getPluginId());
			}
		}
		return ids;
	}

	private void closeLoader(URLClassLoader classLoader) {
		classLoaders.remove(classLoader);
		try {
			classLoader.close();
		} catch (Exception e) {
			LOG.debug("Failed to close temporary plugin class loader", e);
		}
	}

	private static URL toURL(Path pluginPath) {
		try {
			return pluginPath.toUri().toURL();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	private static ClassLoader thisClassLoader() {
		return JadxExternalPluginsLoader.class.getClassLoader();
	}

	@Override
	public void close() {
		try {
			for (URLClassLoader classLoader : classLoaders) {
				try {
					classLoader.close();
				} catch (Exception e) {
					// ignore
				}
			}
		} finally {
			classLoaders.clear();
			loadedClassLoaders.clear();
		}
	}

	private static final class PendingPluginEntry {
		final Path path;
		final String parentPluginId;
		final Set<String> pluginIds;

		PendingPluginEntry(Path path, String parentPluginId, Set<String> pluginIds) {
			this.path = path;
			this.parentPluginId = parentPluginId;
			this.pluginIds = pluginIds;
		}
	}
}
