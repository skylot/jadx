package jadx.api;

public enum ResourceType {
	CODE(".dex", ".jar", ".class"),
	MANIFEST("AndroidManifest.xml"),
	XML(".xml"),
	ARSC(".arsc"),
	FONT(".ttf"),
	IMG(".png", ".gif", ".jpg"),
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
				if (fileName.endsWith(ext)) {
					return type;
				}
			}
		}
		return UNKNOWN;
	}

	public static boolean isSupportedForUnpack(ResourceType type) {
		switch (type) {
			case CODE:
			case LIB:
			case FONT:
			case UNKNOWN:
				return false;

			case MANIFEST:
			case XML:
			case ARSC:
			case IMG:
				return true;
		}
		return false;
	}
}
