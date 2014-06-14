package jadx.gui.utils;

import java.util.Locale;
import java.util.ResourceBundle;

public class NLS {

	private static ResourceBundle messages;

	static {
		load(new Locale("en", "US"));
	}

	private NLS() {
	}

	private static void load(Locale locale) {
		messages = ResourceBundle.getBundle("i18n/Messages", locale);
	}

	public static String str(String key) {
		return messages.getString(key);
	}
}
