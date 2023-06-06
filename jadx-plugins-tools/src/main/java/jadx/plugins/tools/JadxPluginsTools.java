package jadx.plugins.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dev.dirs.ProjectDirectories;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginInfo;
import jadx.core.utils.files.FileUtils;
import jadx.plugins.tools.data.JadxInstalledPlugins;
import jadx.plugins.tools.data.JadxPluginMetadata;
import jadx.plugins.tools.data.JadxPluginUpdate;
import jadx.plugins.tools.resolvers.IJadxPluginResolver;
import jadx.plugins.tools.resolvers.ResolversRegistry;

import static jadx.core.utils.files.FileUtils.makeDirs;

public class JadxPluginsTools {
	private static final JadxPluginsTools INSTANCE = new JadxPluginsTools();

	public static JadxPluginsTools getInstance() {
		return INSTANCE;
	}

	private final Path pluginsJson;
	private final Path dropins;
	private final Path installed;

	private JadxPluginsTools() {
		ProjectDirectories jadxDirs = ProjectDirectories.from("io.github", "skylot", "jadx");
		Path plugins = Paths.get(jadxDirs.configDir, "plugins");
		makeDirs(plugins);
		pluginsJson = plugins.resolve("plugins.json");
		dropins = plugins.resolve("dropins");
		makeDirs(dropins);
		installed = plugins.resolve("installed");
		makeDirs(installed);
	}

	public JadxPluginMetadata install(String locationId) {
		JadxPluginMetadata pluginMetadata = ResolversRegistry.resolve(locationId)
				.orElseThrow(() -> new RuntimeException("Failed to resolve locationId: " + locationId));
		install(pluginMetadata);
		return pluginMetadata;
	}

	public List<JadxPluginUpdate> updateAll() {
		JadxInstalledPlugins plugins = loadPluginsJson();
		int size = plugins.getInstalled().size();
		List<JadxPluginUpdate> updates = new ArrayList<>(size);
		List<JadxPluginMetadata> newList = new ArrayList<>(size);
		for (JadxPluginMetadata plugin : plugins.getInstalled()) {
			JadxPluginMetadata newVersion = update(plugin);
			if (newVersion != null) {
				updates.add(new JadxPluginUpdate(plugin, newVersion));
				newList.add(newVersion);
			} else {
				newList.add(plugin);
			}
		}
		if (!updates.isEmpty()) {
			plugins.setUpdated(System.currentTimeMillis());
			plugins.setInstalled(newList);
			savePluginsJson(plugins);
		}
		return updates;
	}

	public Optional<JadxPluginUpdate> update(String pluginId) {
		JadxInstalledPlugins plugins = loadPluginsJson();
		JadxPluginMetadata plugin = plugins.getInstalled().stream()
				.filter(p -> p.getPluginId().equals(pluginId))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Plugin not found: " + pluginId));

		JadxPluginMetadata newVersion = update(plugin);
		if (newVersion == null) {
			return Optional.empty();
		}
		plugins.setUpdated(System.currentTimeMillis());
		plugins.getInstalled().remove(plugin);
		plugins.getInstalled().add(newVersion);
		savePluginsJson(plugins);
		return Optional.of(new JadxPluginUpdate(plugin, newVersion));
	}

	public boolean uninstall(String pluginId) {
		JadxInstalledPlugins plugins = loadPluginsJson();
		Optional<JadxPluginMetadata> found = plugins.getInstalled().stream()
				.filter(p -> p.getPluginId().equals(pluginId))
				.findFirst();
		if (found.isEmpty()) {
			return false;
		}
		JadxPluginMetadata plugin = found.get();
		deletePluginJar(plugin);
		plugins.getInstalled().remove(plugin);
		savePluginsJson(plugins);
		return true;
	}

	private void deletePluginJar(JadxPluginMetadata plugin) {
		try {
			Files.deleteIfExists(installed.resolve(plugin.getJar()));
		} catch (IOException e) {
			// ignore
		}
	}

	public List<JadxPluginMetadata> getInstalled() {
		return loadPluginsJson().getInstalled();
	}

	public List<Path> getAllPluginJars() {
		List<Path> list = new ArrayList<>();
		for (JadxPluginMetadata pluginMetadata : loadPluginsJson().getInstalled()) {
			list.add(installed.resolve(pluginMetadata.getJar()));
		}
		collectFromDir(list, dropins);
		return list;
	}

	private @Nullable JadxPluginMetadata update(JadxPluginMetadata plugin) {
		IJadxPluginResolver resolver = ResolversRegistry.getById(plugin.getResolverId());
		if (!resolver.isUpdateSupported()) {
			return null;
		}
		Optional<JadxPluginMetadata> updateOpt = resolver.resolve(plugin.getLocationId());
		if (updateOpt.isEmpty()) {
			return null;
		}
		JadxPluginMetadata update = updateOpt.get();
		if (update.getVersion().equals(plugin.getVersion())) {
			return null;
		}
		install(update);
		return update;
	}

	private void install(JadxPluginMetadata metadata) {
		Path tmpJar;
		if (needDownload(metadata.getJar())) {
			tmpJar = FileUtils.createTempFile("plugin.jar");
			downloadJar(metadata.getJar(), tmpJar);
		} else {
			tmpJar = Paths.get(metadata.getJar());
		}
		fillPluginInfoFromJar(metadata, tmpJar);

		String version = metadata.getVersion();
		String fileName = metadata.getPluginId() + (version != null ? '-' + version : "") + ".jar";
		Path pluginJar = installed.resolve(fileName);
		copyJar(tmpJar, pluginJar);
		metadata.setJar(installed.relativize(pluginJar).toString());

		JadxInstalledPlugins plugins = loadPluginsJson();
		// remove previous version jar
		plugins.getInstalled().stream()
				.filter(p -> p.getPluginId().equals(metadata.getPluginId()))
				.forEach(this::deletePluginJar);
		plugins.getInstalled().remove(metadata);
		plugins.getInstalled().add(metadata);
		plugins.setUpdated(System.currentTimeMillis());
		savePluginsJson(plugins);
	}

	private void fillPluginInfoFromJar(JadxPluginMetadata metadata, Path jar) {
		try (JadxExternalPluginsLoader loader = new JadxExternalPluginsLoader()) {
			JadxPlugin jadxPlugin = loader.loadFromJar(jar);
			JadxPluginInfo pluginInfo = jadxPlugin.getPluginInfo();
			metadata.setPluginId(pluginInfo.getPluginId());
			metadata.setName(pluginInfo.getName());
			metadata.setDescription(pluginInfo.getDescription());
		}
	}

	private boolean needDownload(String jar) {
		return jar.startsWith("https://") || jar.startsWith("http://");
	}

	private void downloadJar(String sourceJar, Path destPath) {
		try (InputStream in = URI.create(sourceJar).toURL().openStream()) {
			Files.copy(in, destPath, StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			throw new RuntimeException("Failed to download jar: " + sourceJar, e);
		}
	}

	private void copyJar(Path sourceJar, Path destJar) {
		try {
			Files.copy(sourceJar, destJar, StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			throw new RuntimeException("Failed to copy plugin jar: " + sourceJar + " to: " + destJar, e);
		}
	}

	private static Gson buildGson() {
		return new GsonBuilder().setPrettyPrinting().create();
	}

	private JadxInstalledPlugins loadPluginsJson() {
		if (!Files.isRegularFile(pluginsJson)) {
			return new JadxInstalledPlugins();
		}
		try (Reader reader = Files.newBufferedReader(pluginsJson, StandardCharsets.UTF_8)) {
			return buildGson().fromJson(reader, JadxInstalledPlugins.class);
		} catch (Exception e) {
			throw new RuntimeException("Failed to read file: " + pluginsJson);
		}
	}

	private void savePluginsJson(JadxInstalledPlugins data) {
		if (data.getInstalled().isEmpty()) {
			try {
				Files.deleteIfExists(pluginsJson);
			} catch (Exception e) {
				throw new RuntimeException("Failed to remove file: " + pluginsJson, e);
			}
			return;
		}
		data.getInstalled().sort(null);
		try (Writer writer = Files.newBufferedWriter(pluginsJson, StandardCharsets.UTF_8)) {
			buildGson().toJson(data, writer);
		} catch (Exception e) {
			throw new RuntimeException("Error saving file: " + pluginsJson, e);
		}
	}

	private static void collectFromDir(List<Path> list, Path dir) {
		try (Stream<Path> files = Files.list(dir)) {
			files.filter(p -> p.getFileName().toString().endsWith(".jar")).forEach(list::add);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
