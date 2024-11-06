package jadx.plugins.tools;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
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

public class JadxExternalPluginsLoader implements JadxPluginLoader {
	private static final Logger LOG = LoggerFactory.getLogger(JadxExternalPluginsLoader.class);

	private final List<URLClassLoader> classLoaders = new ArrayList<>();

	@Override
	public List<JadxPlugin> load() {
		close();
		long start = System.currentTimeMillis();
		Map<Class<? extends JadxPlugin>, JadxPlugin> map = new HashMap<>();
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

	public JadxPlugin loadFromJar(Path jar) {
		Map<Class<? extends JadxPlugin>, JadxPlugin> map = new HashMap<>();
		loadFromJar(map, jar);
		int loaded = map.size();
		if (loaded == 0) {
			throw new JadxRuntimeException("No plugin found in jar: " + jar);
		}
		if (loaded > 1) {
			String plugins = map.values().stream().map(p -> p.getPluginInfo().getPluginId()).collect(Collectors.joining(", "));
			throw new JadxRuntimeException("Expect only one plugin per jar: " + jar + ", but found: " + loaded + " - " + plugins);
		}
		return Utils.first(map.values());

	}

	private void loadFromClsLoader(Map<Class<? extends JadxPlugin>, JadxPlugin> map, ClassLoader classLoader) {
		ServiceLoader.load(JadxPlugin.class, classLoader)
				.stream()
				.filter(p -> p.type().getClassLoader() == classLoader)
				.filter(p -> !map.containsKey(p.type()))
				.forEach(p -> map.put(p.type(), p.get()));
	}

	private void loadInstalledPlugins(Map<Class<? extends JadxPlugin>, JadxPlugin> map) {
		List<Path> jars = JadxPluginsTools.getInstance().getEnabledPluginJars();
		for (Path jar : jars) {
			loadFromJar(map, jar);
		}
	}

	private void loadFromJar(Map<Class<? extends JadxPlugin>, JadxPlugin> map, Path jar) {
		try {
			File jarFile = jar.toFile();
			String clsLoaderName = "jadx-plugin:" + jarFile.getName();
			URL[] urls = new URL[] { jarFile.toURI().toURL() };
			URLClassLoader pluginClsLoader = new URLClassLoader(clsLoaderName, urls, thisClassLoader());
			classLoaders.add(pluginClsLoader);
			loadFromClsLoader(map, pluginClsLoader);
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to load plugins from jar: " + jar, e);
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
