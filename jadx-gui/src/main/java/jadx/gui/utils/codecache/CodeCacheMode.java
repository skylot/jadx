package jadx.gui.utils.codecache;

public enum CodeCacheMode {
	// AUTO("Use memory until it is available, use disk if at limit"), // TODO
	MEMORY("Everything in memory: fast search, slow reopen, high memory usage"),
	DISK_WITH_INDEX("Code saved on disk with in memory index: medium search, fast reopen, medium memory usage"),
	DISK("Everything on disk: slow search, fast reopen, low memory usage");

	private final String desc;

	CodeCacheMode(String desc) {
		this.desc = desc;
	}

	public String getDesc() {
		return desc;
	}
}
