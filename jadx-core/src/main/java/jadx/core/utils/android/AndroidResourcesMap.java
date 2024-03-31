package jadx.core.utils.android;

import java.io.InputStream;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import jadx.core.utils.exceptions.JadxRuntimeException;

/**
 * Store resources id to name mapping
 */
public class AndroidResourcesMap {
	private static final Map<Integer, String> RES_MAP = loadBundled();

	public static @Nullable String getResName(int resId) {
		return RES_MAP.get(resId);
	}

	public static Map<Integer, String> getMap() {
		return RES_MAP;
	}

	private static Map<Integer, String> loadBundled() {
		try (InputStream is = AndroidResourcesMap.class.getResourceAsStream("/android/res-map.txt")) {
			return TextResMapFile.read(is);
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to load android resource file (res-map.txt)", e);
		}
	}
}
