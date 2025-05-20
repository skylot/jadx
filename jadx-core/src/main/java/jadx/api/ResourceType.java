package jadx.api;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jadx.core.utils.exceptions.JadxRuntimeException;

public enum ResourceType {
	CODE(".dex", ".jar", ".class"),
	XML(".xml"),
	ARSC(".arsc"),
	APK(".apk", ".apkm", ".apks"),
	FONT(".ttf", ".ttc", ".otf"),
	IMG(".png", ".gif", ".jpg", ".webp", ".bmp", ".tiff"),
	ARCHIVE(".zip", ".rar", ".7zip", ".7z", ".arj", ".tar", ".gzip", ".bzip", ".bzip2", ".cab", ".cpio", ".ar", ".gz", ".tgz", ".bz2"),
	VIDEOS(".mp4", ".mkv", ".webm", ".avi", ".flv", ".3gp"),
	SOUNDS(".aac", ".ogg", ".opus", ".mp3", ".wav", ".wma", ".mid", ".midi"),
	JSON(".json"),
	TEXT(".txt", ".ini", ".conf", ".yaml", ".properties", ".js"),
	HTML(".html"),
	LIB(".so"),
	MANIFEST,
	UNKNOWN;

	private final String[] exts;

	ResourceType(String... exts) {
		this.exts = exts;
	}

	public String[] getExts() {
		return exts;
	}

	private static final Map<String, ResourceType> EXT_MAP = new HashMap<>();

	static {
		for (ResourceType type : ResourceType.values()) {
			for (String ext : type.getExts()) {
				ResourceType prev = EXT_MAP.put(ext, type);
				if (prev != null) {
					throw new JadxRuntimeException("Duplicate extension in ResourceType: " + ext);
				}
			}
		}
	}

	public static ResourceType getFileType(String fileName) {
		if (fileName.endsWith("/resources.pb")) {
			return ARSC;
		}
		int dot = fileName.lastIndexOf('.');
		if (dot != -1) {
			String ext = fileName.substring(dot).toLowerCase(Locale.ROOT);
			ResourceType resType = EXT_MAP.get(ext);
			if (resType != null) {
				if (resType == XML && fileName.equals("AndroidManifest.xml")) {
					return MANIFEST;
				}
				return resType;
			}
		}
		return UNKNOWN;
	}
}
