package jadx.plugins.tools.utils;

public class PluginsUtils {

	public static String removePrefix(String str, String prefix) {
		if (str.startsWith(prefix)) {
			return str.substring(prefix.length());
		}
		return str;
	}
}
