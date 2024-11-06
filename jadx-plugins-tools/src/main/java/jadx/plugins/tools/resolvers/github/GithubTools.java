package jadx.plugins.tools.resolvers.github;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import jadx.plugins.tools.resolvers.github.data.Release;

public class GithubTools {
	private static final String GITHUB_API_URL = "https://api.github.com/";

	private static final Type RELEASE_TYPE = new TypeToken<Release>() {
	}.getType();
	private static final Type RELEASE_LIST_TYPE = new TypeToken<List<Release>>() {
	}.getType();

	public static Release fetchRelease(LocationInfo info) {
		String projectUrl = GITHUB_API_URL + "repos/" + info.getOwner() + "/" + info.getProject();
		String version = info.getVersion();
		if (version == null) {
			// get latest version
			return get(projectUrl + "/releases/latest", RELEASE_TYPE);
		}
		// search version in other releases (by name)
		List<Release> releases = fetchReleases(info, 1, 50);
		return releases.stream()
				.filter(r -> r.getName().equals(version))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Release with version: " + version + " not found."
						+ " Available versions: " + releases.stream().map(Release::getName).collect(Collectors.joining(", "))));
	}

	public static List<Release> fetchReleases(LocationInfo info, int page, int perPage) {
		String projectUrl = GITHUB_API_URL + "repos/" + info.getOwner() + "/" + info.getProject();
		String requestUrl = projectUrl + "/releases?page=" + page + "&per_page=" + perPage;
		return get(requestUrl, RELEASE_LIST_TYPE);
	}

	private static <T> T get(String url, Type type) {
		HttpURLConnection con;
		try {
			con = (HttpURLConnection) URI.create(url).toURL().openConnection();
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
}
