package jadx.gui.utils;

import java.util.prefs.Preferences;

public class JadxPreferences {

	public static String getLastOpenFilePath() {
		String result = "";

		try {
			result = getPreferences().get(KEY_LAST_OPEN_FILE_PATH, "");

			if (result.isEmpty())
			{
				result = System.getProperty("user.home");
			}
		}
		catch (Exception anyEx) {
			/* do nothing, no preferences */
		}

		return result;
	}

	public static void putLastOpenFilePath(String path) {
		try {
			Preferences prefs = getPreferences();

			prefs.put(KEY_LAST_OPEN_FILE_PATH, path);
			prefs.sync();
		}
		catch (Exception anyEx) {
			/* do nothing, no preferences */
		}
	}

	public static String getLastSaveFilePath() {
		String result = "";

		try {
			result = getPreferences().get(KEY_LAST_SAVE_FILE_PATH, "");
			if (result.isEmpty())
			{
				result = getLastOpenFilePath();
			}
		}
		catch (Exception anyEx) {
			/* do nothing, no preferences */
		}

		return result;
	}

	public static void putLastSaveFilePath(String path) {
		try {
			Preferences prefs = getPreferences();

			prefs.put(KEY_LAST_SAVE_FILE_PATH, path);
			prefs.sync();
		}
		catch (Exception anyEx) {
			/* do nothing, no preferences */
		}
	}

	public static boolean getFlattenPackage() {
		boolean result = false;

		try {
			Preferences prefs = getPreferences();

			result = prefs.getBoolean(KEY_FLATTEN_PACKAGE, false);
		}
		catch (Exception anyEx) {
			/* do nothing, no preferences */
		}

		return result;
	}

	public static void putFlattenPackage(boolean value) {
		try {
			Preferences prefs = getPreferences();

			prefs.putBoolean(KEY_FLATTEN_PACKAGE, value);
			prefs.sync();
		}
		catch (Exception anyEx) {
			/* do nothing, no preferences */
		}
	}

	private static final String KEY_LAST_OPEN_FILE_PATH = "lastOpenFilePath";
	private static final String KEY_LAST_SAVE_FILE_PATH = "lastSaveFilePath";
	private static final String KEY_FLATTEN_PACKAGE = "flattenPackage";

	private static Preferences prefs = null;

	private static Preferences getPreferences() {
		if (prefs == null) {
			prefs = Preferences.userRoot();
		}
		return prefs;
	}

}
