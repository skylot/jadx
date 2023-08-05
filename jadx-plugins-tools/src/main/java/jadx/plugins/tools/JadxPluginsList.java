package jadx.plugins.tools;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import jadx.api.plugins.utils.ZipSecurity;
import jadx.core.utils.files.FileUtils;
import jadx.plugins.tools.data.JadxPluginMetadata;
import jadx.plugins.tools.resolvers.github.GithubTools;
import jadx.plugins.tools.resolvers.github.LocationInfo;
import jadx.plugins.tools.resolvers.github.data.Asset;
import jadx.plugins.tools.resolvers.github.data.Release;
import jadx.plugins.tools.utils.PluginUtils;

/**
 * TODO: implement list caching (on disk) with check for new release
 */
public class JadxPluginsList {
	private static final JadxPluginsList INSTANCE = new JadxPluginsList();

	private static final Type LIST_TYPE = new TypeToken<List<JadxPluginMetadata>>() {
	}.getType();

	public static JadxPluginsList getInstance() {
		return INSTANCE;
	}

	private @Nullable List<JadxPluginMetadata> cache;

	private JadxPluginsList() {
	}

	public synchronized List<JadxPluginMetadata> fetch() {
		if (cache != null) {
			return cache;
		}
		LocationInfo latest = new LocationInfo("jadx-decompiler", "jadx-plugins-list", "list", null);
		Release release = GithubTools.fetchRelease(latest);
		List<Asset> assets = release.getAssets();
		if (assets.isEmpty()) {
			throw new RuntimeException("Release don't have assets");
		}
		Asset listAsset = assets.get(0);
		Path tmpListFile = FileUtils.createTempFile("list.zip");
		PluginUtils.downloadFile(listAsset.getDownloadUrl(), tmpListFile);

		List<JadxPluginMetadata> entries = loadListBundle(tmpListFile);
		cache = entries;
		return entries;
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

	@TestOnly
	public synchronized List<JadxPluginMetadata> fetchFromLocalBundle(Path bundleFile) {
		if (cache != null) {
			return cache;
		}
		List<JadxPluginMetadata> entries = loadListBundle(bundleFile);
		cache = entries;
		return entries;
	}
}
