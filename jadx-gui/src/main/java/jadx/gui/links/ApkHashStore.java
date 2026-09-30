package jadx.gui.links;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import jadx.core.utils.GsonUtils;
import jadx.gui.utils.files.JadxFiles;

/**
 * Persistent SHA-256 history of opened APK files (hash, path, last opened), keyed by path.
 */
public class ApkHashStore {
	private static final Logger LOG = LoggerFactory.getLogger(ApkHashStore.class);
	private static final Gson GSON = GsonUtils.buildGson();
	private static final Type LIST_TYPE = new TypeToken<List<Entry>>() {
	}.getType();

	public static class Entry {
		String hash;
		String path;
		long lastOpened;

		public String getHash() {
			return hash;
		}

		public String getPath() {
			return path;
		}

		public long getLastOpened() {
			return lastOpened;
		}
	}

	private final Path storeFile;
	private final Map<String, Entry> byPath;

	public ApkHashStore() {
		this(JadxFiles.APK_HASHES);
	}

	public ApkHashStore(Path storeFile) {
		this.storeFile = storeFile;
		this.byPath = load();
	}

	public static boolean isApk(Path path) {
		Path name = path.getFileName();
		return name != null && name.toString().toLowerCase(Locale.ROOT).endsWith(".apk");
	}

	/**
	 * Hash the file, store/update its entry, return the hash.
	 */
	public synchronized String record(Path file) throws Exception {
		Entry e = new Entry();
		e.path = file.toAbsolutePath().normalize().toString();
		e.hash = calcHash(file);
		e.lastOpened = System.currentTimeMillis();
		byPath.put(e.path, e);
		save();
		return e.hash;
	}

	/**
	 * Most recently opened file whose hash starts with {@code prefix} and whose current
	 * content still matches (re-hashed to be sure). Null if none.
	 */
	public synchronized @Nullable Path findByHashPrefix(String prefix) {
		String p = prefix.toLowerCase(Locale.ROOT);
		for (Entry e : entries()) {
			if (!e.hash.startsWith(p)) {
				continue;
			}
			Path path = Path.of(e.path);
			try {
				if (Files.isRegularFile(path) && calcHash(path).equals(e.hash)) {
					return path;
				}
			} catch (Exception ex) {
				// unreadable, skip
			}
		}
		return null;
	}

	/** Entries sorted by last opened, newest first. */
	public synchronized List<Entry> entries() {
		List<Entry> list = new ArrayList<>(byPath.values());
		list.sort(Comparator.comparingLong((Entry e) -> e.lastOpened).reversed());
		return list;
	}

	public synchronized void remove(String path) {
		if (byPath.remove(path) != null) {
			save();
		}
	}

	public static String calcHash(Path file) throws Exception {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		byte[] buf = new byte[64 * 1024];
		try (InputStream in = Files.newInputStream(file)) {
			int n;
			while ((n = in.read(buf)) != -1) {
				md.update(buf, 0, n);
			}
		}
		StringBuilder sb = new StringBuilder(64);
		for (byte b : md.digest()) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

	private Map<String, Entry> load() {
		Map<String, Entry> map = new LinkedHashMap<>();
		try {
			if (Files.exists(storeFile)) {
				List<Entry> list = GSON.fromJson(Files.readString(storeFile, StandardCharsets.UTF_8), LIST_TYPE);
				if (list != null) {
					for (Entry e : list) {
						if (e.path != null && e.hash != null) {
							map.put(e.path, e);
						}
					}
				}
			}
		} catch (Exception e) {
			LOG.error("Failed to load APK hashes from: {}", storeFile, e);
		}
		return map;
	}

	private void save() {
		try {
			Files.createDirectories(storeFile.getParent());
			Files.writeString(storeFile, GSON.toJson(entries(), LIST_TYPE), StandardCharsets.UTF_8);
		} catch (Exception e) {
			LOG.error("Failed to save APK hashes to: {}", storeFile, e);
		}
	}
}
