package jadx.plugins.tools;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.loader.JadxBasePluginLoader;
import jadx.api.plugins.utils.CommonFileUtils;
import jadx.core.Jadx;
import jadx.core.plugins.versions.VerifyRequiredVersion;
import jadx.core.utils.StringUtils;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.FileUtils;
import jadx.plugins.tools.data.JadxInstalledPlugins;
import jadx.plugins.tools.data.JadxPluginListEntry;
import jadx.plugins.tools.data.JadxPluginMetadata;
import jadx.plugins.tools.data.JadxPluginUpdate;
import jadx.plugins.tools.resolvers.IJadxPluginResolver;
import jadx.plugins.tools.resolvers.ResolversRegistry;
import jadx.plugins.tools.utils.PluginUtils;
import jadx.zip.IZipEntry;
import jadx.zip.ZipContent;
import jadx.zip.ZipReader;

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

	private final Path pluginsJson;
	private final Path installedDir;
	private final Path dropinsDir;
	private final Supplier<List<JadxPluginListEntry>> pluginsListSupplier;

	private JadxPluginsTools() {
		this(PLUGINS_JSON, INSTALLED_DIR, DROPINS_DIR, () -> JadxPluginsList.getInstance().get());
	}

	public JadxPluginsTools(Path pluginsJson, Path installedDir, Path dropinsDir,
			Supplier<List<JadxPluginListEntry>> pluginsListSupplier) {
		this.pluginsJson = pluginsJson;
		this.installedDir = installedDir;
		this.dropinsDir = dropinsDir;
		this.pluginsListSupplier = pluginsListSupplier;
		FileUtils.makeDirs(installedDir);
		FileUtils.makeDirs(dropinsDir);
	}

	public JadxPluginMetadata install(String locationId) {
		List<JadxPluginMetadata> installed = installPlugins(locationId);
		return installed.get(installed.size() - 1);
	}

	/**
	 * Install plugin from locationId along with any missing dependency or parent plugins.
	 *
	 * @return list of installed or re-enabled plugins (dependencies first, requested plugin last)
	 */
	public List<JadxPluginMetadata> installPlugins(String locationId) {
		InstallSession session = new InstallSession();
		installLocation(locationId, session);
		return session.installedPlugins;
	}

	private JadxPluginMetadata installLocation(String locationId, InstallSession session) {
		if (session.locationStack.contains(locationId)) {
			throw new JadxRuntimeException("Circular plugin dependency detected: "
					+ formatCycle(session.locationStack, locationId));
		}
		IJadxPluginResolver resolver = ResolversRegistry.getResolver(locationId);
		Supplier<List<JadxPluginMetadata>> fetchVersions;
		if (resolver.hasVersion(locationId)) {
			fetchVersions = () -> {
				JadxPluginMetadata version = resolver.resolve(locationId)
						.orElseThrow(() -> new JadxRuntimeException("Failed to resolve plugin location: " + locationId));
				return Collections.singletonList(version);
			};
		} else {
			// load latest 10 version to search for compatible one
			fetchVersions = () -> resolver.resolveVersions(locationId, 1, 10);
		}
		List<JadxPluginMetadata> versionsMetadata;
		try {
			versionsMetadata = fetchVersions.get();
		} catch (Exception e) {
			throw new JadxRuntimeException("Plugin info fetch failed, locationId: " + locationId, e);
		}
		if (versionsMetadata.isEmpty()) {
			throw new JadxRuntimeException("Plugin release not found, locationId: " + locationId);
		}
		VerifyRequiredVersion verifyRequiredVersion = new VerifyRequiredVersion();
		List<String> rejectedVersions = new ArrayList<>();
		for (JadxPluginMetadata pluginMetadata : versionsMetadata) {
			// download plugin jar and fill metadata
			// any download or plugin instantiation errors will stop versions check
			fillMetadata(pluginMetadata);
			if (verifyRequiredVersion.isCompatible(pluginMetadata.getRequiredJadxVersion())) {
				installMetadata(pluginMetadata, session, locationId);
				return pluginMetadata;
			}
			String pluginVersion = Utils.getOrElse(pluginMetadata.getVersion(), "unknown");
			rejectedVersions.add(" version '" + pluginVersion + "' not compatible, require: "
					+ pluginMetadata.getRequiredJadxVersion());
		}
		throw new JadxRuntimeException("Can't find compatible version to install"
				+ ", current jadx version: " + verifyRequiredVersion.getJadxVersion()
				+ "\nrejected plugin versions:\n"
				+ String.join("\n", rejectedVersions));
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
		List<JadxPluginMetadata> snapshot = new ArrayList<>(plugins.getInstalled());
		List<JadxPluginUpdate> updates = new ArrayList<>(snapshot.size());
		for (JadxPluginMetadata plugin : snapshot) {
			JadxPluginMetadata newVersion = null;
			try {
				newVersion = update(plugin);
			} catch (Exception e) {
				LOG.warn("Failed to update plugin: {}", plugin.getPluginId(), e);
			}
			if (newVersion != null) {
				updates.add(new JadxPluginUpdate(plugin, newVersion));
			}
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
		warnIfRequiredByEnabledPlugins(plugins, pluginId);
		JadxPluginMetadata plugin = found.get();
		deletePlugin(plugins, plugin);
		plugins.getInstalled().remove(plugin);
		savePluginsJson(plugins);
		return true;
	}

	private boolean removeInstalledPlugin(String pluginId) {
		JadxInstalledPlugins plugins = loadPluginsJson();
		Optional<JadxPluginMetadata> found = plugins.getInstalled().stream()
				.filter(p -> p.getPluginId().equals(pluginId))
				.findFirst();
		if (found.isEmpty()) {
			return false;
		}
		JadxPluginMetadata plugin = found.get();
		deletePlugin(plugins, plugin);
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
		try (JadxExternalPluginsLoader pluginsLoader = new JadxExternalPluginsLoader(c -> true, this)) {
			return pluginsLoader.load().stream()
					.map(JadxPlugin::getPluginInfo)
					.collect(Collectors.toList());
		}
	}

	public List<JadxPluginMetadata> getEnabledInstalled() {
		JadxInstalledPlugins plugins = loadPluginsJson();
		deleteLeftoverFiles(plugins);
		return plugins.getInstalled().stream()
				.filter(p -> !p.isDisabled())
				.collect(Collectors.toList());
	}

	public List<Path> getDropinsPaths() {
		return FileUtils.listFiles(dropinsDir);
	}

	public Path getInstalledPluginPath(JadxPluginMetadata pluginMetadata) {
		return installedDir.resolve(pluginMetadata.getPath());
	}

	public List<Path> getEnabledPluginPaths() {
		List<JadxPluginMetadata> enabled = getEnabledInstalled();
		List<Path> list = new ArrayList<>(enabled.size());
		for (JadxPluginMetadata pluginMetadata : enabled) {
			list.add(getInstalledPluginPath(pluginMetadata));
		}
		list.addAll(getDropinsPaths());
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
		if (disabled) {
			warnIfRequiredByEnabledPlugins(data, pluginId);
		} else {
			warnIfRequiresDisabledPlugins(data, plugin);
		}
		plugin.setDisabled(disabled);
		data.setUpdated(System.currentTimeMillis());
		savePluginsJson(data);
		return true;
	}

	private static void warnIfRequiredByEnabledPlugins(JadxInstalledPlugins data, String pluginId) {
		List<String> dependents = data.getInstalled().stream()
				.filter(p -> !p.isDisabled() && !pluginId.equals(p.getPluginId()))
				.filter(p -> pluginId.equals(p.getParentPlugin())
						|| (p.getDependencyPlugins() != null && p.getDependencyPlugins().contains(pluginId)))
				.map(JadxPluginMetadata::getPluginId)
				.collect(Collectors.toList());
		if (!dependents.isEmpty()) {
			LOG.warn("Plugin '{}' is required by enabled plugin(s): {}", pluginId, String.join(", ", dependents));
		}
	}

	private static void warnIfRequiresDisabledPlugins(JadxInstalledPlugins data, JadxPluginMetadata plugin) {
		List<String> requiredIds = collectDependencies(plugin);
		if (requiredIds.isEmpty()) {
			return;
		}
		Set<String> disabledIds = data.getInstalled().stream()
				.filter(JadxPluginMetadata::isDisabled)
				.map(JadxPluginMetadata::getPluginId)
				.collect(Collectors.toSet());
		List<String> disabledDeps = requiredIds.stream()
				.filter(disabledIds::contains)
				.collect(Collectors.toList());
		if (!disabledDeps.isEmpty()) {
			LOG.warn("Enabled plugin '{}' requires disabled plugin(s): {}",
					plugin.getPluginId(), String.join(", ", disabledDeps));
		}
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
		installMetadata(update, new InstallSession(), update.getLocationId());
		return update;
	}

	private void installMetadata(JadxPluginMetadata metadata, InstallSession session, String activeLocation) {
		String reqVersionStr = metadata.getRequiredJadxVersion();
		if (!VerifyRequiredVersion.isJadxCompatible(reqVersionStr)) {
			throw new JadxRuntimeException("Can't install plugin, required version: \"" + reqVersionStr + '\"'
					+ " is not compatible with current jadx version: " + Jadx.getVersion());
		}
		String pluginId = metadata.getPluginId();
		if (session.pluginIdStack.contains(pluginId)) {
			throw new JadxRuntimeException("Circular plugin dependency detected: "
					+ formatCycle(session.pluginIdStack, pluginId));
		}
		session.locationStack.add(activeLocation);
		session.pluginIdStack.add(pluginId);
		try {
			for (String dep : collectDependencies(metadata)) {
				ensureDependencyInstalled(dep, metadata, session);
			}
			// remove previous version of this plugin
			removeInstalledPlugin(pluginId);

			String version = metadata.getVersion();
			String pluginBaseName = pluginId + (StringUtils.notBlank(version) ? '-' + version : "");
			String pluginPathStr = metadata.getPath();
			Path pluginPath = Paths.get(pluginPathStr);
			if (pluginPathStr.endsWith(".jar")) {
				Path pluginJar = newPluginPath(pluginBaseName, ".jar");
				copyJar(pluginPath, pluginJar);
				metadata.setPath(installedDir.relativize(pluginJar).toString());
			} else if (Files.isDirectory(pluginPath)) {
				Path pluginDir = newPluginPath(pluginBaseName, "");
				try {
					org.apache.commons.io.FileUtils.moveDirectory(pluginPath.toFile(), pluginDir.toFile());
				} catch (IOException e) {
					throw new JadxRuntimeException("Failed to install plugin: " + pluginBaseName, e);
				}
				metadata.setPath(installedDir.relativize(pluginDir).toString());
			} else {
				throw new JadxRuntimeException("Unexpected plugin path type: " + pluginPathStr);
			}
			// update plugins json
			JadxInstalledPlugins plugins = loadPluginsJson();
			plugins.getToDelete().remove(metadata.getPath());
			plugins.getInstalled().add(metadata);
			plugins.setUpdated(System.currentTimeMillis());
			savePluginsJson(plugins);
			session.verifiedPluginIds.add(pluginId);
			session.installedPlugins.add(metadata);
		} finally {
			session.locationStack.remove(session.locationStack.size() - 1);
			session.pluginIdStack.remove(session.pluginIdStack.size() - 1);
		}
	}

	private static List<String> collectDependencies(JadxPluginMetadata metadata) {
		Set<String> deps = new LinkedHashSet<>();
		if (metadata.getDependencyPlugins() != null) {
			deps.addAll(metadata.getDependencyPlugins());
		}
		if (metadata.getParentPlugin() != null) {
			deps.add(metadata.getParentPlugin());
		}
		return new ArrayList<>(deps);
	}

	private void ensureDependencyInstalled(String dep, JadxPluginMetadata parentMeta, InstallSession session) {
		boolean isLocationId = dep.indexOf(':') > 0;
		if (isLocationId) {
			ensureLocationDependencyInstalled(dep, session);
			return;
		}
		String pluginId = parentMeta.getPluginId();
		if (session.pluginIdStack.contains(dep)) {
			throw new JadxRuntimeException("Circular plugin dependency detected: "
					+ formatCycle(session.pluginIdStack, dep));
		}
		if (getBundledPluginIds(session).contains(dep)) {
			return;
		}
		JadxPluginMetadata installedDep = findInstalledById(dep);
		if (installedDep != null) {
			ensureInstalledMetadataSatisfied(installedDep, session);
			return;
		}
		if (getDropinPluginIds(session).contains(dep)) {
			return;
		}
		Map<String, String> depLocations = parentMeta.getDependencyLocations();
		String depLocation = depLocations != null ? depLocations.get(dep) : null;
		if (depLocation == null) {
			depLocation = resolveLocationFromList(dep);
		}
		if (depLocation == null) {
			throw new JadxRuntimeException("Failed to find dependency plugin '" + dep + "' for plugin '" + pluginId + '\'');
		}
		LOG.info("Installing dependency plugin '{}' ({}) for '{}'", dep, depLocation, pluginId);
		JadxPluginMetadata installed = installLocation(depLocation, session);
		if (!dep.equals(installed.getPluginId())) {
			throw new JadxRuntimeException("Expected dependency pluginId '" + dep
					+ "', but got '" + installed.getPluginId() + "' from " + depLocation);
		}
	}

	private void ensureLocationDependencyInstalled(String depLocationId, InstallSession session) {
		IJadxPluginResolver resolver = ResolversRegistry.getResolver(depLocationId);
		if (!resolver.hasVersion(depLocationId)) {
			JadxPluginMetadata existing = loadPluginsJson().getInstalled().stream()
					.filter(m -> depLocationId.equals(m.getLocationId()))
					.findFirst()
					.orElse(null);
			if (existing != null) {
				ensureInstalledMetadataSatisfied(existing, session);
				return;
			}
		}
		LOG.info("Installing dependency plugin from location '{}'", depLocationId);
		installLocation(depLocationId, session);
	}

	private void ensureInstalledMetadataSatisfied(JadxPluginMetadata metadata, InstallSession session) {
		String pluginId = metadata.getPluginId();
		if (session.pluginIdStack.contains(pluginId)) {
			throw new JadxRuntimeException("Circular plugin dependency detected: "
					+ formatCycle(session.pluginIdStack, pluginId));
		}
		if (!session.verifiedPluginIds.add(pluginId)) {
			return;
		}
		List<String> subDeps = collectDependencies(metadata);
		if (!subDeps.isEmpty()) {
			session.pluginIdStack.add(pluginId);
			try {
				for (String subDep : subDeps) {
					ensureDependencyInstalled(subDep, metadata, session);
				}
			} finally {
				session.pluginIdStack.remove(session.pluginIdStack.size() - 1);
			}
		}
		if (metadata.isDisabled()) {
			changeDisabledStatus(pluginId, false);
			LOG.info("Enabled required dependency plugin: {}", pluginId);
			metadata.setDisabled(false);
			session.installedPlugins.add(metadata);
		}
	}

	private @Nullable String resolveLocationFromList(String pluginId) {
		List<JadxPluginListEntry> entries = pluginsListSupplier.get();
		if (entries == null) {
			return null;
		}
		return entries.stream()
				.filter(entry -> pluginId.equals(entry.getPluginId()))
				.map(JadxPluginListEntry::getLocationId)
				.findFirst()
				.orElse(null);
	}

	private @Nullable JadxPluginMetadata findInstalledById(String pluginId) {
		return loadPluginsJson().getInstalled().stream()
				.filter(p -> pluginId.equals(p.getPluginId()))
				.findFirst()
				.orElse(null);
	}

	private Set<String> getBundledPluginIds(InstallSession session) {
		if (session.bundledIds == null) {
			Set<String> ids = new HashSet<>();
			for (JadxPlugin plugin : new JadxBasePluginLoader().load()) {
				ids.add(plugin.getPluginInfo().getPluginId());
			}
			session.bundledIds = ids;
		}
		return session.bundledIds;
	}

	private Set<String> getDropinPluginIds(InstallSession session) {
		if (session.dropinIds == null) {
			Set<String> ids = new HashSet<>();
			List<Path> dropins = getDropinsPaths();
			if (!dropins.isEmpty()) {
				try (JadxExternalPluginsLoader loader = new JadxExternalPluginsLoader(c -> true, this)) {
					for (Path dropinPath : dropins) {
						try {
							for (JadxPlugin plugin : loader.loadMetadataPluginsFromPath(dropinPath)) {
								ids.add(plugin.getPluginInfo().getPluginId());
							}
						} catch (Exception e) {
							LOG.debug("Failed to inspect dropin plugin: {}", dropinPath, e);
						}
					}
				}
			}
			session.dropinIds = ids;
		}
		return session.dropinIds;
	}

	private static String formatCycle(List<String> stack, String duplicate) {
		int idx = stack.indexOf(duplicate);
		List<String> cycle = idx >= 0 ? new ArrayList<>(stack.subList(idx, stack.size())) : new ArrayList<>(stack);
		cycle.add(duplicate);
		return String.join(" -> ", cycle);
	}

	private Path newPluginPath(String baseName, String ext) {
		while (true) {
			String suffix = String.format("%06x", ThreadLocalRandom.current().nextInt(0x1000000));
			Path path = installedDir.resolve(baseName + '-' + suffix + ext);
			if (!Files.exists(path)) {
				return path;
			}
		}
	}

	private void fillMetadata(JadxPluginMetadata metadata) {
		try {
			String pluginPath = metadata.getPath();
			if (needDownload(pluginPath)) {
				// download plugin
				String ext = CommonFileUtils.getFileExtension(pluginPath);
				Path tmpJar = Files.createTempFile(metadata.getName(), "plugin." + ext);
				PluginUtils.downloadFile(pluginPath, tmpJar);
				pluginPath = tmpJar.toAbsolutePath().toString();
			}
			if (pluginPath.endsWith(".zip")) {
				// unpack plugin zip
				Path tmpDir = Files.createTempDirectory(metadata.getName());
				unzip(Paths.get(pluginPath), tmpDir);
				pluginPath = tmpDir.toAbsolutePath().toString();
			}
			metadata.setPath(pluginPath);
			fillMetadataFromPath(metadata, Paths.get(pluginPath));
		} catch (Exception e) {
			throw new RuntimeException("Failed to fill plugin metadata, plugin: " + metadata.getPluginId(), e);
		}
	}

	private void fillMetadataFromPath(JadxPluginMetadata metadata, Path pluginPath) {
		try (JadxExternalPluginsLoader loader = new JadxExternalPluginsLoader(c -> true, this)) {
			JadxPlugin jadxPlugin = loader.loadFromPath(pluginPath);
			JadxPluginInfo pluginInfo = jadxPlugin.getPluginInfo();
			metadata.setPluginId(pluginInfo.getPluginId());
			metadata.setName(pluginInfo.getName());
			metadata.setDescription(pluginInfo.getDescription());
			metadata.setHomepage(pluginInfo.getHomepage());
			metadata.setRequiredJadxVersion(pluginInfo.getRequiredJadxVersion());
			metadata.setParentPlugin(pluginInfo.getParentPlugin());
			List<String> deps = pluginInfo.getDependencyPlugins();
			metadata.setDependencyPlugins(deps.isEmpty() ? null : new ArrayList<>(deps));
			Map<String, String> depLocations = pluginInfo.getDependencyLocations();
			metadata.setDependencyLocations(depLocations.isEmpty() ? null : new LinkedHashMap<>(depLocations));
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

	private void deletePlugin(JadxInstalledPlugins plugins, JadxPluginMetadata plugin) {
		String path = plugin.getPath();
		if (!deletePluginFiles(path)) {
			// on Windows, files of loaded plugins can not be deleted
			LOG.warn("Failed to delete plugin files, will try again on next load: {}", installedDir.resolve(path));
			plugins.getToDelete().add(path);
		}
	}

	public void deleteLeftoverFiles() {
		deleteLeftoverFiles(loadPluginsJson());
	}

	private void deleteLeftoverFiles(JadxInstalledPlugins plugins) {
		if (plugins.getToDelete().removeIf(this::deletePluginFiles)) {
			savePluginsJson(plugins);
		}
	}

	private boolean deletePluginFiles(String path) {
		Path pluginPath = installedDir.resolve(path);
		try {
			if (Files.isDirectory(pluginPath)) {
				FileUtils.deleteDir(pluginPath);
			} else {
				Files.deleteIfExists(pluginPath);
			}
		} catch (Exception e) {
			LOG.debug("Failed to delete plugin files: {}", pluginPath, e);
		}
		return !Files.exists(pluginPath);
	}

	private JadxInstalledPlugins loadPluginsJson() {
		if (!Files.isRegularFile(pluginsJson)) {
			JadxInstalledPlugins plugins = new JadxInstalledPlugins();
			plugins.setVersion(1);
			return plugins;
		}
		try (Reader reader = Files.newBufferedReader(pluginsJson, StandardCharsets.UTF_8)) {
			JadxInstalledPlugins data = buildGson().fromJson(reader, JadxInstalledPlugins.class);
			upgradePluginsData(data);
			return data;
		} catch (Exception e) {
			throw new RuntimeException("Failed to read file: " + pluginsJson);
		}
	}

	private void savePluginsJson(JadxInstalledPlugins data) {
		if (data.getInstalled().isEmpty() && data.getToDelete().isEmpty()) {
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

	private void upgradePluginsData(JadxInstalledPlugins data) {
		if (data.getVersion() == 0) {
			data.setVersion(1);
		}
	}

	private static void unzip(Path zipFile, Path outDir) {
		ZipReader zipReader = new ZipReader(); // TODO: pass zip options from jadx args
		Path normalizedOutDir = outDir.toAbsolutePath().normalize();
		try (ZipContent content = zipReader.open(zipFile.toFile())) {
			for (IZipEntry entry : content.getEntries()) {
				Path entryFile = normalizedOutDir.resolve(entry.getName()).normalize();
				if (!entryFile.startsWith(normalizedOutDir)) {
					throw new JadxRuntimeException("Invalid zip entry path: " + entry.getName());
				}
				if (entry.isDirectory()) {
					Files.createDirectories(entryFile);
					continue;
				}
				Path parent = entryFile.getParent();
				if (parent != null) {
					Files.createDirectories(parent);
				}
				try (InputStream in = entry.getInputStream()) {
					Files.copy(in, entryFile, StandardCopyOption.REPLACE_EXISTING);
				}
			}
		} catch (IOException e) {
			throw new JadxRuntimeException("Failed to unzip file: " + zipFile, e);
		}
	}

	private static final class InstallSession {
		final List<JadxPluginMetadata> installedPlugins = new ArrayList<>();
		final List<String> locationStack = new ArrayList<>();
		final List<String> pluginIdStack = new ArrayList<>();
		final Set<String> verifiedPluginIds = new HashSet<>();
		@Nullable
		Set<String> bundledIds;
		@Nullable
		Set<String> dropinIds;
	}
}
