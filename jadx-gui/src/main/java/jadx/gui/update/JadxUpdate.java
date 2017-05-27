package jadx.gui.update;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxDecompiler;
import jadx.gui.update.data.Release;

public class JadxUpdate {
	private static final Logger LOG = LoggerFactory.getLogger(JadxUpdate.class);

	public static final String JADX_RELEASES_URL = "https://github.com/skylot/jadx/releases";

	private static final String GITHUB_API_URL = "https://api.github.com/";
	private static final String GITHUB_RELEASES_URL = GITHUB_API_URL + "repos/skylot/jadx/releases";

	private static final Gson GSON = new Gson();

	private static final Type RELEASES_LIST_TYPE = new TypeToken<List<Release>>() {
	}.getType();

	private static final Comparator<Release> RELEASE_COMPARATOR = new Comparator<Release>() {
		@Override
		public int compare(Release o1, Release o2) {
			return VersionComparator.checkAndCompare(o1.getName(), o2.getName());
		}
	};

	public interface IUpdateCallback {
		void onUpdate(Release r);
	}

	private JadxUpdate() {
	}

	public static void check(final IUpdateCallback callback) {
		Runnable run = new Runnable() {
			@Override
			public void run() {
				try {
					Release release = checkForNewRelease();
					if (release != null) {
						callback.onUpdate(release);
					}
				} catch (Exception e) {
					LOG.debug("Jadx update error", e);
				}
			}
		};
		Thread thread = new Thread(run);
		thread.setName("Jadx update thread");
		thread.setPriority(Thread.MIN_PRIORITY);
		thread.start();
	}

	private static Release checkForNewRelease() throws IOException {
		String version = JadxDecompiler.getVersion();
		if (version.contains("dev")) {
			LOG.debug("Ignore check for update: development version");
			return null;
		}

		List<Release> list = get(GITHUB_RELEASES_URL, RELEASES_LIST_TYPE);
		if (list == null) {
			return null;
		}
		for (Iterator<Release> it = list.iterator(); it.hasNext(); ) {
			Release release = it.next();
			if (release.getName().equalsIgnoreCase(version)
					|| release.isPreRelease()) {
				it.remove();
			}
		}
		if (list.isEmpty()) {
			return null;
		}
		Collections.sort(list, RELEASE_COMPARATOR);
		Release latest = list.get(list.size() - 1);
		if (VersionComparator.checkAndCompare(version, latest.getName()) >= 0) {
			return null;
		}
		LOG.info("Found new jadx version: {}", latest);
		return latest;
	}

	private static <T> T get(String url, Type type) throws IOException {
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("GET");
		if (con.getResponseCode() == 200) {
			Reader reader = new InputStreamReader(con.getInputStream(), "UTF-8");
			return GSON.fromJson(reader, type);
		}
		return null;
	}
}
