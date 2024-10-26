package jadx.plugins.tools;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import jadx.api.plugins.utils.ZipSecurity;
import jadx.core.utils.files.FileUtils;
import jadx.plugins.tools.data.JadxPluginListCache;
import jadx.plugins.tools.data.JadxPluginMetadata;
import jadx.plugins.tools.resolvers.github.GithubTools;
import jadx.plugins.tools.resolvers.github.LocationInfo;
import jadx.plugins.tools.resolvers.github.data.Asset;
import jadx.plugins.tools.resolvers.github.data.Release;
import jadx.plugins.tools.utils.PluginUtils;

import static jadx.plugins.tools.utils.PluginFiles.PLUGINS_LIST_CACHE;

public class JadxPluginsList {
	private static final JadxPluginsList INSTANCE = new JadxPluginsList();

	private static final Type LIST_TYPE = new TypeToken<List<JadxPluginMetadata>>() {
	}.getType();

	private static final Type CACHE_TYPE = new TypeToken<JadxPluginListCache>() {
	}.getType();

	public static JadxPluginsList getInstance() {
		return INSTANCE;
	}

	private @Nullable JadxPluginListCache loadedList;

	private JadxPluginsList() {
	}

	/**
	 * List provider with update callback.
	 * Can be called one or two times:
	 * <br>
	 * - Apply cached data first
	 * <br>
	 * - If update is available, apply data after fetch
	 * <br>
	 * Method call is blocking.
	 */
	public synchronized void get(Consumer<List<JadxPluginMetadata>> consumer) {
		if (loadedList != null) {
			consumer.accept(loadedList.getList());
			return;
		}
		JadxPluginListCache listCache = loadCache();
		if (listCache != null) {
			consumer.accept(listCache.getList());
		}
		Release release = fetchLatestRelease();
		if (listCache == null || !listCache.getVersion().equals(release.getName())) {
			JadxPluginListCache updatedList = fetchBundle(release);
			saveCache(updatedList);
			consumer.accept(updatedList.getList());
		}
	}

	public List<JadxPluginMetadata> get() {
		AtomicReference<List<JadxPluginMetadata>> holder = new AtomicReference<>();
		get(holder::set);
		return holder.get();
	}

	private @Nullable JadxPluginListCache loadCache() {
		if (!Files.isRegularFile(PLUGINS_LIST_CACHE)) {
			return null;
		}
		try {
			String jsonStr = FileUtils.readFile(PLUGINS_LIST_CACHE);
			return buildGson().fromJson(jsonStr, CACHE_TYPE);
		} catch (Exception e) {
			return null;
		}
	}

	private void saveCache(JadxPluginListCache listCache) {
		try {
			String jsonStr = buildGson().toJson(listCache, CACHE_TYPE);
			FileUtils.writeFile(PLUGINS_LIST_CACHE, jsonStr);
		} catch (Exception e) {
			throw new RuntimeException("Error saving file: " + PLUGINS_LIST_CACHE, e);
		}
		loadedList = listCache;
	}

	private static Gson buildGson() {
		return new GsonBuilder().setPrettyPrinting().create();
	}

	private Release fetchLatestRelease() {
		LocationInfo latest = new LocationInfo("jadx-decompiler", "jadx-plugins-list", "list", null);
		Release release = GithubTools.fetchRelease(latest);
		List<Asset> assets = release.getAssets();
		if (assets.isEmpty()) {
			throw new RuntimeException("Release don't have assets");
		}
		return release;
	}

	private JadxPluginListCache fetchBundle(Release release) {
		Asset listAsset = release.getAssets().get(0);
		Path tmpListFile = FileUtils.createTempFile("list.zip");
		PluginUtils.downloadFile(listAsset.getDownloadUrl(), tmpListFile);

		JadxPluginListCache listCache = new JadxPluginListCache();
		listCache.setVersion(release.getName());
		listCache.setList(loadListBundle(tmpListFile));
		return listCache;
	}

	private static List<JadxPluginMetadata> loadListBundle(Path tmpListFile) {
		Gson gson = new Gson();
		List<JadxPluginMetadata> entries = new ArrayList<>();
		ZipSecurity.readZipEntries(tmpListFile.toFile(), (entry, in) -> {
			if (entry.getName().endsWith(".json")) {
				try (Reader reader = new InputStreamReader(in)) {
					entries.addAll(gson.fromJson(reader, LIST_TYPE));
				} catch (Exception e) {
					throw new RuntimeException("Failed to read plugins list entry: " + entry.getName());
				}
			}
		});
		return entries;
	}
}
