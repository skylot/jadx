package jadx.gui.cache.code;

import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO: use localized strings
public enum CodeCacheMode {
	MEMORY("Everything in memory: fast search, slow reopen, high memory usage"),
	DISK_WITH_CACHE("Code saved on disk with in memory cache: medium search, fast reopen, medium memory usage"),
	DISK("Everything on disk: slow search, fast reopen, low memory usage");

	private final String desc;

	CodeCacheMode(String desc) {
		this.desc = desc;
	}

	public String getDesc() {
		return desc;
	}

	public static String buildToolTip() {
		return Stream.of(values())
				.map(v -> v.name() + " - " + v.getDesc())
				.collect(Collectors.joining("\n"));
	}
}
