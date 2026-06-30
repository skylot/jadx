package jadx.commons.app;

import org.jetbrains.annotations.Nullable;

public class JadxCommonEnv {

	public static @Nullable String get(String varName, @Nullable String defValue) {
		String strValue = System.getenv(varName);
		return isNullOrEmpty(strValue) ? defValue : strValue;
	}

	public static boolean getBool(String varName, boolean defValue) {
		String strValue = System.getenv(varName);
		if (isNullOrEmpty(strValue)) {
			return defValue;
		}
		return strValue.equalsIgnoreCase("true");
	}

	public static int getInt(String varName, int defValue) {
		String strValue = System.getenv(varName);
		if (isNullOrEmpty(strValue)) {
			return defValue;
		}
		return Integer.parseInt(strValue);
	}

	private static boolean isNullOrEmpty(@Nullable String value) {
		return value == null || value.isEmpty();
	}
}
