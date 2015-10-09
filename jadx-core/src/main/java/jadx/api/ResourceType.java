package jadx.api;

public enum ResourceType {
	CODE(".dex", ".class"),
	MANIFEST("AndroidManifest.xml"),
	XML(".xml"), // TODO binary or not?
	ARSC(".arsc"), // TODO decompile !!!
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
			case IMG:
			case UNKNOWN:
				return false;

			case MANIFEST:
			case XML:
			case ARSC:
				return true;
		}
		return false;
	}
}
