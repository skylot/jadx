package jadx.gui.utils;

import java.util.Locale;
import java.util.ResourceBundle;

public class NLS {

	private static ResourceBundle messages;
	private static final boolean IS_CHINESE = Locale.SIMPLIFIED_CHINESE==Locale.getDefault();

	static {
        if(IS_CHINESE){
            load(Locale.SIMPLIFIED_CHINESE);
        }else {
            load(new Locale("en", "US"));
        }
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
