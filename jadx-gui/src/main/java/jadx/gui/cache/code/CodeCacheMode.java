package jadx.gui.cache.code;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import jadx.gui.utils.NLS;

public enum CodeCacheMode {
	MEMORY("preferences.codeCacheMode.memory", "preferences.codeCacheMode.memory.desc"),
	DISK_WITH_CACHE("preferences.codeCacheMode.diskWithCache", "preferences.codeCacheMode.diskWithCache.desc"),
	DISK("preferences.codeCacheMode.disk", "preferences.codeCacheMode.disk.desc");

	private final String labelKey;
	private final String descKey;

	CodeCacheMode(String labelKey, String descKey) {
		this.labelKey = labelKey;
		this.descKey = descKey;
	}

	public String getLocalizedName() {
		return NLS.str(labelKey);
	}

	public String getDesc() {
		return NLS.str(descKey);
	}

	@Override
	public String toString() {
		return getLocalizedName();
	}

	public static String buildToolTip() {
		return Stream.of(values())
				.map(v -> v.getLocalizedName() + " - " + v.getDesc())
				.collect(Collectors.joining("\n"));
	}
}
