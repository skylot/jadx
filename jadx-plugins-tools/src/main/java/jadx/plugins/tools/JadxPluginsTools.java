package jadx.plugins.tools;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginInfo;
import jadx.core.Jadx;
import jadx.core.plugins.versions.VerifyRequiredVersion;
import jadx.core.utils.StringUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.FileUtils;
import jadx.plugins.tools.data.JadxInstalledPlugins;
import jadx.plugins.tools.data.JadxPluginMetadata;
import jadx.plugins.tools.data.JadxPluginUpdate;
import jadx.plugins.tools.resolvers.IJadxPluginResolver;
import jadx.plugins.tools.resolvers.ResolversRegistry;
import jadx.plugins.tools.utils.PluginUtils;

import static jadx.core.utils.GsonUtils.buildGson;
import static jadx.plugins.tools.utils.PluginFiles.DROPINS_DIR;
import static jadx.plugins.tools.utils.PluginFiles.INSTALLED_DIR;
import static jadx.plugins.tools.utils.PluginFiles.PLUGINS_JSON;

public class JadxPluginsTools {
	private static final Logger LOG = LoggerFactory.getLogger(JadxPluginsTools.class);

	private static final JadxPluginsTools INSTANCE = new JadxPluginsTools();

	public static JadxPluginsTools getInstance() {
		return INSTANCE;
	}

	private JadxPluginsTools() {
	}

	public JadxPluginMetadata install(String locationId) {
		IJadxPluginResolver resolver = ResolversRegistry.getResolver(locationId);
		boolean hasVersion = resolver.hasVersion(locationId);
		if (hasVersion) {
			JadxPluginMetadata pluginMetadata = resolver.resolve(locationId)
					.orElseThrow(() -> new JadxRuntimeException("Failed to resolve plugin location: " + locationId));
			fillMetadata(pluginMetadata);
			install(pluginMetadata);
			return pluginMetadata;
		}
		// try other versions in case latest is not compatible with current jadx
		VerifyRequiredVersion verifyRequiredVersion = new VerifyRequiredVersion();
		for (int i = 1; i <= 5; i++) {
			try {
				for (JadxPluginMetadata pluginMetadata : resolver.resolveVersions(locationId, i, 1)) {
					fillMetadata(pluginMetadata);
					if (verifyRequiredVersion.isCompatible(pluginMetadata.getRequiredJadxVersion())) {
						install(pluginMetadata);
						return pluginMetadata;
					}
				}
			} catch (Exception e) {
				LOG.warn("Failed to fetch plugin ({} version before latest)", i, e);
			}
		}
		throw new JadxRuntimeException("Can't find compatible version to install");
	}

	public JadxPluginMetadata resolveMetadata(String locationId) {
		IJadxPluginResolver resolver = ResolversRegistry.getResolver(locationId);
		JadxPluginMetadata pluginMetadata = resolver.resolve(locationId)
				.orElseThrow(() -> new RuntimeException("Failed to resolve locationId: " + locationId));
		fillMetadata(pluginMetadata);
		return pluginMetadata;
	}

	public List<JadxPluginMetadata> getVersionsByLocation(String locationId, int page, int perPage) {
		IJadxPluginResolver resolver = ResolversRegistry.getResolver(locationId);
		List<JadxPluginMetadata> list = resolver.resolveVersions(locationId, page, perPage);
		for (JadxPluginMetadata pluginMetadata : list) {
			fillMetadata(pluginMetadata);
		}
		return list;
	}

	public List<JadxPluginUpdate> updateAll() {
		JadxInstalledPlugins plugins = loadPluginsJson();
		int size = plugins.getInstalled().size();
		List<JadxPluginUpdate> updates = new ArrayList<>(size);
		List<JadxPluginMetadata> newList = new ArrayList<>(size);
		for (JadxPluginMetadata plugin : plugins.getInstalled()) {
			JadxPluginMetadata newVersion = null;
			try {
				newVersion = update(plugin);
			} catch (Exception e) {
				LOG.warn("Failed to update plugin: {}", plugin.getPluginId(), e);
			}
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

	public List<JadxPluginMetadata> getInstalled() {
		return loadPluginsJson().getInstalled();
	}

	/**
	 * Return all loadable plugins info (including installed, bundled and dropins).
	 * <br>
	 * For only installed plugins prefer {@link jadx.plugins.tools.JadxPluginsTools#getInstalled}
	 * method.
	 */
	public List<JadxPluginInfo> getAllPluginsInfo() {
		try (JadxExternalPluginsLoader pluginsLoader = new JadxExternalPluginsLoader()) {
			return pluginsLoader.load().stream()
					.map(JadxPlugin::getPluginInfo)
					.collect(Collectors.toList());
		}
	}

	public List<Path> getAllPluginJars() {
		List<Path> list = new ArrayList<>();
		for (JadxPluginMetadata pluginMetadata : loadPluginsJson().getInstalled()) {
			list.add(INSTALLED_DIR.resolve(pluginMetadata.getJar()));
		}
		collectJarsFromDir(list, DROPINS_DIR);
		return list;
	}

	public List<Path> getEnabledPluginJars() {
		List<Path> list = new ArrayList<>();
		for (JadxPluginMetadata pluginMetadata : loadPluginsJson().getInstalled()) {
			if (pluginMetadata.isDisabled()) {
				continue;
			}
			list.add(INSTALLED_DIR.resolve(pluginMetadata.getJar()));
		}
		collectJarsFromDir(list, DROPINS_DIR);
		return list;
	}

	/**
	 * Disable or enable plugin
	 *
	 * @return true if disabled status was changed
	 */
	public boolean changeDisabledStatus(String pluginId, boolean disabled) {
		JadxInstalledPlugins data = loadPluginsJson();
		JadxPluginMetadata plugin = data.getInstalled().stream()
				.filter(p -> p.getPluginId().equals(pluginId))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Plugin not found: " + pluginId));
		if (plugin.isDisabled() == disabled) {
			return false;
		}
		plugin.setDisabled(disabled);
		data.setUpdated(System.currentTimeMillis());
		savePluginsJson(data);
		return true;
	}

	private @Nullable JadxPluginMetadata update(JadxPluginMetadata plugin) {
		IJadxPluginResolver resolver = ResolversRegistry.getResolver(plugin.getLocationId());
		if (!resolver.isUpdateSupported()) {
			return null;
		}
		Optional<JadxPluginMetadata> updateOpt = resolver.resolve(plugin.getLocationId());
		if (updateOpt.isEmpty()) {
			return null;
		}
		JadxPluginMetadata update = updateOpt.get();
		if (Objects.equals(update.getVersion(), plugin.getVersion())) {
			return null;
		}
		fillMetadata(update);
		install(update);
		return update;
	}

	private void install(JadxPluginMetadata metadata) {
		String reqVersionStr = metadata.getRequiredJadxVersion();
		if (!VerifyRequiredVersion.isJadxCompatible(reqVersionStr)) {
			throw new JadxRuntimeException("Can't install plugin, required version: \"" + reqVersionStr + '\"'
					+ " is not compatible with current jadx version: " + Jadx.getVersion());
		}

		String version = metadata.getVersion();
		String fileName = metadata.getPluginId() + (StringUtils.notBlank(version) ? '-' + version : "") + ".jar";
		Path pluginJar = INSTALLED_DIR.resolve(fileName);
		copyJar(Paths.get(metadata.getJar()), pluginJar);
		metadata.setJar(INSTALLED_DIR.relativize(pluginJar).toString());

		JadxInstalledPlugins plugins = loadPluginsJson();
		// remove previous version jar
		plugins.getInstalled().removeIf(p -> {
			if (p.getPluginId().equals(metadata.getPluginId())) {
				deletePluginJar(p);
				return true;
			}
			return false;
		});
		plugins.getInstalled().add(metadata);
		plugins.setUpdated(System.currentTimeMillis());
		savePluginsJson(plugins);
	}

	private void fillMetadata(JadxPluginMetadata metadata) {
		Path tmpJar;
		if (needDownload(metadata.getJar())) {
			tmpJar = FileUtils.createTempFile("plugin.jar");
			PluginUtils.downloadFile(metadata.getJar(), tmpJar);
			metadata.setJar(tmpJar.toAbsolutePath().toString());
		} else {
			tmpJar = Paths.get(metadata.getJar());
		}
		fillMetadataFromJar(metadata, tmpJar);
	}

	private void fillMetadataFromJar(JadxPluginMetadata metadata, Path jar) {
		try (JadxExternalPluginsLoader loader = new JadxExternalPluginsLoader()) {
			JadxPlugin jadxPlugin = loader.loadFromJar(jar);
			JadxPluginInfo pluginInfo = jadxPlugin.getPluginInfo();
			metadata.setPluginId(pluginInfo.getPluginId());
			metadata.setName(pluginInfo.getName());
			metadata.setDescription(pluginInfo.getDescription());
			metadata.setHomepage(pluginInfo.getHomepage());
			metadata.setRequiredJadxVersion(pluginInfo.getRequiredJadxVersion());
		} catch (NoSuchMethodError e) {
			throw new RuntimeException("Looks like plugin uses unknown API, try to update jadx version", e);
		}
	}

	private static boolean needDownload(String jar) {
		return jar.startsWith("https://") || jar.startsWith("http://");
	}

	private void copyJar(Path sourceJar, Path destJar) {
		try {
			Files.copy(sourceJar, destJar, StandardCopyOption.REPLACE_EXISTING);
		} catch (Exception e) {
			throw new RuntimeException("Failed to copy plugin jar: " + sourceJar + " to: " + destJar, e);
		}
	}

	private void deletePluginJar(JadxPluginMetadata plugin) {
		try {
			Files.deleteIfExists(INSTALLED_DIR.resolve(plugin.getJar()));
		} catch (IOException e) {
			// ignore
		}
	}

	private JadxInstalledPlugins loadPluginsJson() {
		if (!Files.isRegularFile(PLUGINS_JSON)) {
			JadxInstalledPlugins plugins = new JadxInstalledPlugins();
			plugins.setVersion(1);
			return plugins;
		}
		try (Reader reader = Files.newBufferedReader(PLUGINS_JSON, StandardCharsets.UTF_8)) {
			JadxInstalledPlugins data = buildGson().fromJson(reader, JadxInstalledPlugins.class);
			upgradePluginsData(data);
			return data;
		} catch (Exception e) {
			throw new RuntimeException("Failed to read file: " + PLUGINS_JSON);
		}
	}

	private void savePluginsJson(JadxInstalledPlugins data) {
		if (data.getInstalled().isEmpty()) {
			try {
				Files.deleteIfExists(PLUGINS_JSON);
			} catch (Exception e) {
				throw new RuntimeException("Failed to remove file: " + PLUGINS_JSON, e);
			}
			return;
		}
		data.getInstalled().sort(null);
		try (Writer writer = Files.newBufferedWriter(PLUGINS_JSON, StandardCharsets.UTF_8)) {
			buildGson().toJson(data, writer);
		} catch (Exception e) {
			throw new RuntimeException("Error saving file: " + PLUGINS_JSON, e);
		}
	}

	private void upgradePluginsData(JadxInstalledPlugins data) {
		if (data.getVersion() == 0) {
			data.setVersion(1);
		}
	}

	private static void collectJarsFromDir(List<Path> list, Path dir) {
		try (Stream<Path> files = Files.list(dir)) {
			files.filter(p -> p.getFileName().toString().endsWith(".jar")).forEach(list::add);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
