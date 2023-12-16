package jadx.gui.cache.manager;

import java.io.BufferedReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import jadx.api.plugins.utils.CommonFileUtils;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.FileUtils;
import jadx.gui.settings.JadxProject;
import jadx.gui.settings.JadxSettings;
import jadx.gui.settings.data.ProjectData;
import jadx.gui.utils.files.JadxFiles;

public class CacheManager {
	private static final Logger LOG = LoggerFactory.getLogger(CacheManager.class);

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Type CACHES_TYPE = new TypeToken<List<CacheEntry>>() {
	}.getType();

	private final Map<String, CacheEntry> cacheMap;
	private final JadxSettings settings;

	public CacheManager(JadxSettings settings) {
		this.settings = settings;
		this.cacheMap = loadCaches();
	}

	/**
	 * If project cache is set -> check if cache entry exists for this project.
	 * If not -> calculate new and add entry.
	 */
	public Path getCacheDir(JadxProject project, @Nullable String cacheDirStr) {
		if (cacheDirStr == null) {
			Path newProjectCacheDir = buildCacheDir(project);
			addEntry(projectToKey(project), newProjectCacheDir);
			return newProjectCacheDir;
		}
		Path cacheDir = resolveCacheDirStr(cacheDirStr, project.getProjectPath());
		return verifyEntry(project, cacheDir);
	}

	public void projectPathUpdate(JadxProject project, Path newPath) {
		if (Objects.equals(project.getProjectPath(), newPath)) {
			return;
		}
		String key = projectToKey(project);
		CacheEntry prevEntry = cacheMap.remove(key);
		if (prevEntry == null) {
			return;
		}
		CacheEntry newEntry = new CacheEntry();
		newEntry.setProject(pathToString(newPath));
		newEntry.setCache(prevEntry.getCache());
		addEntry(newEntry);
	}

	public List<CacheEntry> getCachesList() {
		List<CacheEntry> list = new ArrayList<>(cacheMap.values());
		Collections.sort(list);
		return list;
	}

	public synchronized void removeCacheEntry(CacheEntry entry) {
		try {
			cacheMap.remove(entry.getProject());
			saveCaches(cacheMap);
			FileUtils.deleteDirIfExists(Paths.get(entry.getCache()));
		} catch (Exception e) {
			LOG.error("Failed to remove cache entry: " + entry.getCache(), e);
		}
	}

	private Path resolveCacheDirStr(String cacheDirStr, Path projectPath) {
		Path path = Paths.get(cacheDirStr);
		if (path.isAbsolute() || projectPath == null) {
			return path;
		}
		return projectPath.resolveSibling(path);
	}

	public String buildCacheDirStr(Path dir) {
		if (Objects.equals(settings.getCacheDir(), ".")) {
			return dir.getFileName().toString();
		}
		return pathToString(dir);
	}

	private Path buildCacheDir(JadxProject project) {
		String cacheDirValue = settings.getCacheDir();
		if (Objects.equals(cacheDirValue, ".")) {
			return buildLocalCacheDir(project);
		}
		Path cacheBaseDir = cacheDirValue == null ? JadxFiles.PROJECTS_CACHE_DIR : Paths.get(cacheDirValue);
		return cacheBaseDir.resolve(buildProjectUniqName(project));
	}

	private static Path buildLocalCacheDir(JadxProject project) {
		Path projectPath = project.getProjectPath();
		if (projectPath != null) {
			return projectPath.resolveSibling(projectPath.getFileName() + ".cache");
		}
		List<Path> files = project.getFilePaths();
		if (files.isEmpty()) {
			throw new JadxRuntimeException("Failed to build local cache dir");
		}
		Path path = files.stream()
				.filter(p -> !p.getFileName().toString().endsWith(".jadx.kts"))
				.findFirst()
				.orElseGet(() -> files.get(0));
		String name = CommonFileUtils.removeFileExtension(path.getFileName().toString());
		return path.resolveSibling(name + ".jadx.cache");
	}

	private Path verifyEntry(JadxProject project, Path cacheDir) {
		boolean cacheExists = Files.exists(cacheDir);
		String key = projectToKey(project);
		CacheEntry entry = cacheMap.get(key);
		if (entry == null) {
			Path newCacheDir = cacheExists ? cacheDir : buildCacheDir(project);
			addEntry(key, newCacheDir);
			return newCacheDir;
		}
		if (entry.getCache().equals(pathToString(cacheDir)) && cacheExists) {
			// same and exists
			return cacheDir;
		}
		// remove previous cache dir
		FileUtils.deleteDirIfExists(Paths.get(entry.getCache()));

		Path newCacheDir = cacheExists ? cacheDir : buildCacheDir(project);
		entry.setCache(pathToString(newCacheDir));
		entry.setTimestamp(System.currentTimeMillis());
		saveCaches(cacheMap);
		return newCacheDir;
	}

	private void addEntry(String projectKey, Path cacheDir) {
		CacheEntry entry = new CacheEntry();
		entry.setProject(projectKey);
		entry.setCache(pathToString(cacheDir));
		addEntry(entry);
	}

	private void addEntry(CacheEntry entry) {
		entry.setTimestamp(System.currentTimeMillis());
		cacheMap.put(entry.getProject(), entry);
		saveCaches(cacheMap);
	}

	private String projectToKey(JadxProject project) {
		Path projectPath = project.getProjectPath();
		if (projectPath != null) {
			return pathToString(projectPath);
		}
		return "tmp:" + buildProjectUniqName(project);
	}

	private static String buildProjectUniqName(JadxProject project) {
		return project.getName() + "-" + FileUtils.buildInputsHash(project.getFilePaths());
	}

	public static String pathToString(Path path) {
		try {
			return path.toAbsolutePath().normalize().toString();
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to expand path: " + path, e);
		}
	}

	private synchronized Map<String, CacheEntry> loadCaches() {
		List<CacheEntry> list = null;
		if (Files.exists(JadxFiles.CACHES_LIST)) {
			try (BufferedReader reader = Files.newBufferedReader(JadxFiles.CACHES_LIST)) {
				list = GSON.fromJson(reader, CACHES_TYPE);
			} catch (Exception e) {
				LOG.warn("Failed to load caches list", e);
			}
		} else {
			return initFromRecentProjects();
		}
		if (Utils.isEmpty(list)) {
			return new HashMap<>();
		}
		Map<String, CacheEntry> map = new HashMap<>(list.size());
		for (CacheEntry entry : list) {
			map.put(entry.getProject(), entry);
		}
		return map;
	}

	private synchronized void saveCaches(Map<String, CacheEntry> map) {
		List<CacheEntry> list = new ArrayList<>(map.values());
		Collections.sort(list);
		String json = GSON.toJson(list, CACHES_TYPE);
		try {
			Files.writeString(JadxFiles.CACHES_LIST, json, StandardCharsets.UTF_8,
					StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to write caches file", e);
		}
	}

	/**
	 * Load caches info from recent projects list.
	 * Help for initial migration.
	 */
	private Map<String, CacheEntry> initFromRecentProjects() {
		try {
			Map<String, CacheEntry> map = new HashMap<>();
			long t = System.currentTimeMillis();
			for (Path project : settings.getRecentProjects()) {
				try {
					ProjectData data = JadxProject.loadProjectData(project);
					String cacheDir = data.getCacheDir();
					if (cacheDir == null) {
						// no cache dir, ignore
						continue;
					}
					Path cachePath = resolveCacheDirStr(cacheDir, project);
					if (!Files.isDirectory(cachePath)) {
						continue;
					}
					String key = pathToString(project);
					CacheEntry entry = new CacheEntry();
					entry.setProject(key);
					entry.setCache(pathToString(cachePath));
					entry.setTimestamp(t++); // keep projects order
					map.put(key, entry);
				} catch (Exception e) {
					LOG.warn("Failed to load project file: {}", project, e);
				}
			}
			saveCaches(map);
			return map;
		} catch (Exception e) {
			LOG.warn("Failed to fill cache list from recent projects", e);
			return new HashMap<>();
		}
	}
}
