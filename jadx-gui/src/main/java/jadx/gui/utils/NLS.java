package jadx.gui.utils;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Vector;

import org.jetbrains.annotations.NotNull;

public class NLS {
	private static final Charset JAVA_CHARSET = Charset.forName("ISO-8859-1");
	private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

	private static Vector<LangLocale> i18nLocales = new Vector<>();

	private static Map<LangLocale, Map<String, String>> i18nMessagesMap = new HashMap<>();

	// Use these two fields to avoid invoking Map.get() method twice.
	private static Map<String, String> localizedMessagesMap;
	private static Map<String, String> fallbackMessagesMap;

	private static LangLocale currentLocale;
	private static LangLocale localLocale;

	static {
		localLocale = new LangLocale(Locale.getDefault());

		i18nLocales.add(new LangLocale("en", "US")); // As default language
		i18nLocales.add(new LangLocale("zh", "CN"));
		i18nLocales.add(new LangLocale("es", "ES"));

		i18nLocales.forEach(NLS::load);

		LangLocale defLang = i18nLocales.get(0);
		fallbackMessagesMap = i18nMessagesMap.get(defLang);
		localizedMessagesMap = i18nMessagesMap.get(defLang);
	}

	private NLS() {
	}

	private static void load(LangLocale locale) {
		ResourceBundle bundle = ResourceBundle.getBundle("i18n/Messages", locale.get());
		Map<String, String> resMap = new HashMap<>();
		for (String key : bundle.keySet()) {
			String str = bundle.getString(key);
			resMap.put(key, convertCharset(str));
		}
		i18nMessagesMap.put(locale, resMap);
	}

	@NotNull
	private static String convertCharset(String str) {
		return new String(str.getBytes(JAVA_CHARSET), UTF8_CHARSET);
	}

	public static String str(String key) {
		String str = localizedMessagesMap.get(key);
		if (str != null) {
			return str;
		}
		return fallbackMessagesMap.get(key); // definitely exists
	}

	public static String str(String key, LangLocale locale) {
		Map<String, String> strings = i18nMessagesMap.get(locale);
		if (strings != null) {
			String str = strings.get(key);
			if (str != null) {
				return str;
			}
		}
		return fallbackMessagesMap.get(key); // definitely exists
	}

	public static void setLocale(LangLocale locale) {
		if (i18nMessagesMap.containsKey(locale)) {
			currentLocale = locale;
		} else {
			currentLocale = i18nLocales.get(0);
		}
		localizedMessagesMap = i18nMessagesMap.get(currentLocale);
	}

	public static Vector<LangLocale> getI18nLocales() {
		return i18nLocales;
	}

	public static LangLocale currentLocale() {
		return currentLocale;
	}

	public static LangLocale defaultLocale() {
		if (i18nMessagesMap.containsKey(localLocale)) {
			return localLocale;
		}
		// fallback to english if unsupported
		return i18nLocales.get(0);
	}
}
