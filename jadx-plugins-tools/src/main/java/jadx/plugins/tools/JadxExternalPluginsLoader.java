package jadx.plugins.tools;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.loader.JadxPluginLoader;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.FileUtils;

public class JadxExternalPluginsLoader implements JadxPluginLoader {
	private static final Logger LOG = LoggerFactory.getLogger(JadxExternalPluginsLoader.class);

	public static final String JADX_PLUGIN_CLASSLOADER_PREFIX = "jadx-plugin:";

	private final List<URLClassLoader> classLoaders = new ArrayList<>();

	@Override
	public List<JadxPlugin> load() {
		close();
		long start = System.currentTimeMillis();
		Map<String, JadxPlugin> map = new HashMap<>();
		loadFromClsLoader(map, thisClassLoader());
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
		loadFromPath(map, pluginPath);
		int loaded = map.size();
		if (loaded == 0) {
			throw new JadxRuntimeException("No plugin found in jar: " + pluginPath);
		}
		if (loaded > 1) {
			String plugins = map.values().stream().map(p -> p.getPluginInfo().getPluginId()).collect(Collectors.joining(", "));
			throw new JadxRuntimeException("Expect only one plugin per jar: " + pluginPath + ", but found: " + loaded + " - " + plugins);
		}
		return Utils.first(map.values());

	}

	private void loadFromClsLoader(Map<String, JadxPlugin> map, ClassLoader classLoader) {
		ServiceLoader<JadxPlugin> serviceLoader = ServiceLoader.load(JadxPlugin.class, classLoader);
		for (ServiceLoader.Provider<JadxPlugin> provider : serviceLoader.stream().collect(Collectors.toList())) {
			Class<? extends JadxPlugin> pluginClass = provider.type();
			String clsName = pluginClass.getName();
			if (!map.containsKey(clsName)
					&& pluginClass.getClassLoader() == classLoader) {
				map.put(clsName, provider.get());
			}
		}
	}

	private void loadInstalledPlugins(Map<String, JadxPlugin> map) {
		List<Path> paths = JadxPluginsTools.getInstance().getEnabledPluginPaths();
		for (Path pluginPath : paths) {
			loadFromPath(map, pluginPath);
		}
	}

	private void loadFromPath(Map<String, JadxPlugin> map, Path pluginPath) {
		try {
			URL[] urls;
			if (Files.isDirectory(pluginPath)) {
				urls = FileUtils.listFiles(pluginPath, file -> FileUtils.hasExtension(file, ".jar"))
						.stream()
						.map(JadxExternalPluginsLoader::toURL)
						.toArray(URL[]::new);
				if (urls.length == 0) {
					throw new JadxRuntimeException("No jar files found in plugin directory");
				}
			} else if (Files.isRegularFile(pluginPath)) {
				if (FileUtils.hasExtension(pluginPath, ".jar")) {
					urls = new URL[] { toURL(pluginPath) };
				} else {
					throw new JadxRuntimeException("Unexpected plugin file format");
				}
			} else {
				throw new JadxRuntimeException("Plugin file not found");
			}
			String clsLoaderName = JADX_PLUGIN_CLASSLOADER_PREFIX + pluginPath.getFileName();
			URLClassLoader pluginClsLoader = new URLClassLoader(clsLoaderName, urls, thisClassLoader());
			classLoaders.add(pluginClsLoader);
			loadFromClsLoader(map, pluginClsLoader);
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to load plugins from: " + pluginPath, e);
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
		}
	}
}
