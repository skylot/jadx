package jadx.api;

public enum ResourceType {
	CODE(".dex", ".jar", ".class"),
	MANIFEST("AndroidManifest.xml"),
	XML(".xml"),
	ARSC(".arsc"),
	FONT(".ttf", ".otf"),
	IMG(".png", ".gif", ".jpg"),
	MEDIA(".mp3", ".wav"),
	LIB(".so"),
	UNKNOWN;

	private final String[] exts;

	ResourceType(String... exts) {
		this.exts = exts;
	}

	public String[] getExts() {
		return exts;
	}

	public static ResourceType getFileType(String fileName) {
		for (ResourceType type : ResourceType.values()) {
			for (String ext : type.getExts()) {
				if (fileName.toLowerCase().endsWith(ext)) {
					return type;
				}
			}
		}
		return UNKNOWN;
	}
}
