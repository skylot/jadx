package jadx.plugins.tools;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.loader.JadxPluginLoader;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class JadxExternalPluginsLoader implements JadxPluginLoader {
	private static final Logger LOG = LoggerFactory.getLogger(JadxExternalPluginsLoader.class);

	private final List<URLClassLoader> classLoaders = new ArrayList<>();

	@Override
	public List<JadxPlugin> load() {
		close();
		long start = System.currentTimeMillis();
		Map<Class<? extends JadxPlugin>, JadxPlugin> map = new HashMap<>();
		ClassLoader classLoader = JadxPluginsTools.class.getClassLoader();
		loadFromClsLoader(map, classLoader);
		loadInstalledPlugins(map, classLoader);

		List<JadxPlugin> list = new ArrayList<>(map.size());
		list.addAll(map.values());
		list.sort(Comparator.comparing(p -> p.getClass().getSimpleName()));
		if (LOG.isDebugEnabled()) {
			LOG.debug("Collected {} plugins in {}ms", list.size(), System.currentTimeMillis() - start);
		}
		return list;
	}

	/**
	 * TODO: find a better way to load only plugin from single jar without plugins from parent
	 * classloader
	 */
	public JadxPlugin loadFromJar(Path jar) {
		Map<Class<? extends JadxPlugin>, JadxPlugin> map = new HashMap<>();
		ClassLoader classLoader = JadxPluginsTools.class.getClassLoader();
		loadFromClsLoader(map, classLoader);
		Set<Class<? extends JadxPlugin>> clspPlugins = new HashSet<>(map.keySet());
		try (URLClassLoader pluginClassLoader = loadFromJar(map, classLoader, jar)) {
			return map.entrySet().stream()
					.filter(entry -> !clspPlugins.contains(entry.getKey()))
					.findFirst()
					.map(Map.Entry::getValue)
					.orElseThrow(() -> new RuntimeException("No plugin found in jar: " + jar));
		} catch (IOException e) {
			throw new RuntimeException("Failed to load plugin jar: " + jar, e);
		}
	}

	private void loadFromClsLoader(Map<Class<? extends JadxPlugin>, JadxPlugin> map, ClassLoader classLoader) {
		ServiceLoader.load(JadxPlugin.class, classLoader)
				.stream()
				.filter(p -> !map.containsKey(p.type()))
				.forEach(p -> map.put(p.type(), p.get()));
	}

	private void loadInstalledPlugins(Map<Class<? extends JadxPlugin>, JadxPlugin> map, ClassLoader classLoader) {
		List<Path> jars = JadxPluginsTools.getInstance().getEnabledPluginJars();
		for (Path jar : jars) {
			classLoaders.add(loadFromJar(map, classLoader, jar));
		}
	}

	private URLClassLoader loadFromJar(Map<Class<? extends JadxPlugin>, JadxPlugin> map, ClassLoader classLoader, Path jar) {
		try {
			File jarFile = jar.toFile();
			URL[] urls = new URL[] { jarFile.toURI().toURL() };
			URLClassLoader pluginClsLoader = new URLClassLoader("jadx-plugin:" + jarFile.getName(), urls, classLoader);
			loadFromClsLoader(map, pluginClsLoader);
			return pluginClsLoader;
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to load plugins, jar: " + jar, e);
		}
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
