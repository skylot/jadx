package jadx.plugins.tools.resolvers.github;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;

import jadx.core.utils.files.FileUtils;
import jadx.plugins.tools.resolvers.github.data.Release;

import static org.assertj.core.api.Assertions.assertThat;

class GithubToolsTest {
	private static final Logger LOG = LoggerFactory.getLogger(GithubToolsTest.class);

	private MockWebServer server;
	private GithubTools githubTools;

	@BeforeEach
	public void setup() throws IOException {
		server = new MockWebServer();
		server.start();
		String baseUrl = server.url("/").toString();
		githubTools = new GithubTools(baseUrl);
	}

	@AfterEach
	public void close() {
		server.close();
	}

	@Test
	public void getReleaseGood() {
		server.enqueue(new MockResponse.Builder()
				.body(loadFromResource("plugins-list-good.json"))
				.build());

		LocationInfo pluginsList = new LocationInfo("jadx-decompiler", "jadx-plugins-list", "list");
		Release release = githubTools.getRelease(pluginsList);

		LOG.info("Result release: {}", release);
		assertThat(release.getName()).isEqualTo("v15");
		assertThat(release.getAssets()).hasSize(1);
	}

	@Test
	public void getReleaseRateLimit() {
		server.enqueue(new MockResponse.Builder()
				.code(403)
				.addHeader("x-ratelimit-remaining", "0")
				.addHeader("x-ratelimit-reset", Instant.now().plusSeconds(60 * 60).getEpochSecond()) // 1 hour from now
				.body("{}")
				.build());

		LocationInfo pluginsList = new LocationInfo("jadx-decompiler", "jadx-plugins-list", "list");
		Assertions.assertThatThrownBy(() -> githubTools.getRelease(pluginsList))
				.hasMessageContaining("403")
				.hasMessageContaining("Client Error")
				.hasMessageContaining("rate limit reached");
	}

	private static String loadFromResource(String resName) {
		try (InputStream stream = GithubToolsTest.class.getResourceAsStream("/github/" + resName)) {
			return FileUtils.streamToString(stream);
		} catch (IOException e) {
			throw new RuntimeException("Failed to load resource: " + resName, e);
		}
	}
}
