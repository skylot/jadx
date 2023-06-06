package jadx.plugins.tools.resolvers.github;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import jadx.plugins.tools.data.JadxPluginMetadata;
import jadx.plugins.tools.resolvers.IJadxPluginResolver;
import jadx.plugins.tools.resolvers.github.data.Asset;
import jadx.plugins.tools.resolvers.github.data.Release;

import static jadx.plugins.tools.utils.PluginsUtils.removePrefix;

public class GithubReleaseResolver implements IJadxPluginResolver {
	private static final String GITHUB_API_URL = "https://api.github.com/";
	private static final Pattern VERSION_PATTERN = Pattern.compile("v?\\d+\\.\\d+(\\.\\d+)?");

	private static final Type RELEASE_TYPE = new TypeToken<Release>() {
	}.getType();
	private static final Type RELEASE_LIST_TYPE = new TypeToken<List<Release>>() {
	}.getType();

	@Override
	public Optional<JadxPluginMetadata> resolve(String locationId) {
		if (!locationId.startsWith("github:")) {
			return Optional.empty();
		}
		String[] parts = locationId.split(":");
		if (parts.length < 3) {
			return Optional.empty();
		}
		String owner = parts[1];
		String project = parts[2];
		String version = null;
		String artifactPrefix = project;
		if (parts.length >= 4) {
			String part = parts[3];
			if (VERSION_PATTERN.matcher(part).matches()) {
				version = part;
				if (parts.length >= 5) {
					artifactPrefix = parts[4];
				}
			} else {
				artifactPrefix = part;
			}
		}
		Release release = getRelease(owner, project, version);
		String releaseVersion = removePrefix(release.getName(), "v");
		String artifactName = artifactPrefix + '-' + releaseVersion + ".jar";
		Asset asset = release.getAssets().stream()
				.filter(a -> a.getName().equals(artifactName))
				.findFirst()
				.orElse(searchIgnoringVersion(release, artifactPrefix));

		JadxPluginMetadata metadata = new JadxPluginMetadata();
		metadata.setResolverId(id());
		metadata.setVersion(releaseVersion);
		metadata.setLocationId(buildLocationId(owner, project, artifactPrefix)); // exclude version for later updates
		metadata.setJar(asset.getDownloadUrl());
		return Optional.of(metadata);
	}

	private @NotNull Asset searchIgnoringVersion(Release release, String artifactPrefix) {
		return release.getAssets().stream()
				.filter(a -> a.getName().startsWith(artifactPrefix) && a.getName().startsWith(".jar"))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Release artifact with prefix '" + artifactPrefix + "' not found"));
	}

	@NotNull
	private static String buildLocationId(String owner, String project, String artifactPrefix) {
		if (project.equals(artifactPrefix)) {
			return "github:" + owner + ':' + project;
		}
		return "github:" + owner + ':' + project + ':' + artifactPrefix;
	}

	private static Release getRelease(String owner, String project, @Nullable String version) {
		String projectUrl = GITHUB_API_URL + "repos/" + owner + "/" + project;
		if (version == null) {
			// get latest version
			return get(projectUrl + "/releases/latest", RELEASE_TYPE);
		}
		// search version among all releases (by name)
		List<Release> releases = get(projectUrl + "/releases", RELEASE_LIST_TYPE);
		return releases.stream()
				.filter(r -> r.getName().equals(version))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Release with version: " + version + " not found."
						+ " Available versions: " + releases.stream().map(Release::getName).collect(Collectors.joining(", "))));
	}

	private static <T> T get(String url, Type type) {
		HttpURLConnection con;
		try {
			con = (HttpURLConnection) new URL(url).openConnection();
			con.setRequestMethod("GET");
			int code = con.getResponseCode();
			if (code != 200) {
				// TODO: support redirects?
				throw new RuntimeException("Request failed, response: " + code + ", url: " + url);
			}
		} catch (IOException e) {
			throw new RuntimeException("Request failed, url: " + url, e);
		}
		try (Reader reader = new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8)) {
			return new Gson().fromJson(reader, type);
		} catch (Exception e) {
			throw new RuntimeException("Failed to parse response, url: " + url, e);
		}
	}

	@Override
	public String id() {
		return "github-release";
	}

	@Override
	public boolean isUpdateSupported() {
		return true;
	}
}
