package jadx.api;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jadx.api.resources.ResourceContentType;
import jadx.core.utils.exceptions.JadxRuntimeException;

import static jadx.api.resources.ResourceContentType.CONTENT_BINARY;
import static jadx.api.resources.ResourceContentType.CONTENT_TEXT;
import static jadx.api.resources.ResourceContentType.CONTENT_UNKNOWN;

public enum ResourceType {
	CODE(CONTENT_BINARY, ".dex", ".jar", ".class"),
	XML(CONTENT_TEXT, ".xml"),
	ARSC(CONTENT_TEXT, ".arsc"),
	APK(CONTENT_BINARY, ".apk", ".apkm", ".apks"),
	FONT(CONTENT_BINARY, ".ttf", ".ttc", ".otf"),
	IMG(CONTENT_BINARY, ".png", ".gif", ".jpg", ".webp", ".bmp", ".tiff"),
	ARCHIVE(CONTENT_BINARY, ".zip", ".rar", ".7zip", ".7z", ".arj", ".tar", ".gzip", ".bzip", ".bzip2", ".cab", ".cpio", ".ar", ".gz",
			".tgz", ".bz2"),
	VIDEOS(CONTENT_BINARY, ".mp4", ".mkv", ".webm", ".avi", ".flv", ".3gp"),
	SOUNDS(CONTENT_BINARY, ".aac", ".ogg", ".opus", ".mp3", ".wav", ".wma", ".mid", ".midi"),
	JSON(CONTENT_TEXT, ".json"),
	TEXT(CONTENT_TEXT, ".txt", ".ini", ".conf", ".yaml", ".properties", ".js"),
	HTML(CONTENT_TEXT, ".html"),
	LIB(CONTENT_BINARY, ".so"),
	MANIFEST(CONTENT_TEXT),
	UNKNOWN_BIN(CONTENT_BINARY, ".bin"),
	UNKNOWN(CONTENT_UNKNOWN);

	private final ResourceContentType contentType;
	private final String[] exts;

	ResourceType(ResourceContentType contentType, String... exts) {
		this.contentType = contentType;
		this.exts = exts;
	}

	public ResourceContentType getContentType() {
		return contentType;
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
