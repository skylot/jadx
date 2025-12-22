package jadx.plugins.tools.resolvers.github;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.reflect.TypeToken;

import jadx.core.utils.files.FileUtils;
import jadx.plugins.tools.resolvers.github.data.Release;

import static jadx.core.utils.GsonUtils.buildGson;

public class GithubTools {
	private static final GithubTools GITHUB_INSTANCE = new GithubTools("https://api.github.com");

	private static final Type RELEASE_TYPE = new TypeToken<Release>() {
	}.getType();
	private static final Type RELEASE_LIST_TYPE = new TypeToken<List<Release>>() {
	}.getType();

	public static Release fetchRelease(LocationInfo info) {
		return GITHUB_INSTANCE.getRelease(info);
	}

	public static List<Release> fetchReleases(LocationInfo info, int page, int perPage) {
		return GITHUB_INSTANCE.getReleases(info, page, perPage);
	}

	private final String baseUrl;

	GithubTools(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	Release getRelease(LocationInfo info) {
		String projectUrl = baseUrl + "/repos/" + info.getOwner() + "/" + info.getProject();
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

	List<Release> getReleases(LocationInfo info, int page, int perPage) {
		String projectUrl = baseUrl + "/repos/" + info.getOwner() + "/" + info.getProject();
		String requestUrl = projectUrl + "/releases?page=" + page + "&per_page=" + perPage;
		return get(requestUrl, RELEASE_LIST_TYPE);
	}

	private static <T> T get(String url, Type type) {
		HttpURLConnection con = null;
		try {
			try {
				con = (HttpURLConnection) URI.create(url).toURL().openConnection();
				con.setRequestMethod("GET");
				con.setInstanceFollowRedirects(true);
				int code = con.getResponseCode();
				if (code != 200) {
					throw new RuntimeException(buildErrorDetails(con, url));
				}
			} catch (IOException e) {
				throw new RuntimeException("Request failed, url: " + url, e);
			}
			try (Reader reader = new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8)) {
				return buildGson().fromJson(reader, type);
			} catch (Exception e) {
				throw new RuntimeException("Failed to parse response, url: " + url, e);
			}
		} finally {
			if (con != null) {
				con.disconnect();
			}
		}
	}

	private static String buildErrorDetails(HttpURLConnection con, String url) throws IOException {
		String shortMsg = con.getResponseMessage();
		String remainRateLimit = con.getHeaderField("X-RateLimit-Remaining");
		if ("0".equals(remainRateLimit)) {
			String resetTimeMs = con.getHeaderField("X-RateLimit-Reset");
			String timeStr = resetTimeMs != null ? "after " + Instant.ofEpochSecond(Long.parseLong(resetTimeMs)) : "in one hour";
			shortMsg += " (rate limit reached, try again " + timeStr + ')';
		}
		StringBuilder headers = new StringBuilder();
		for (int i = 0;; i++) {
			String value = con.getHeaderField(i);
			if (value == null) {
				break;
			}
			String key = con.getHeaderFieldKey(i);
			if (key != null) {
				headers.append('\n').append(key).append(": ").append(value);
			}
		}
		String responseStr = getResponseString(con);
		return "Request failed: " + con.getResponseCode() + ' ' + shortMsg
				+ "\nURL: " + url
				+ "\nHeaders:" + headers
				+ (responseStr.isEmpty() ? "" : "\nresponse:\n" + responseStr);
	}

	private static String getResponseString(HttpURLConnection con) {
		try (InputStream in = con.getInputStream()) {
			return new String(FileUtils.streamToByteArray(in), StandardCharsets.UTF_8);
		} catch (Exception e) {
			// ignore
			return "";
		}
	}
}
