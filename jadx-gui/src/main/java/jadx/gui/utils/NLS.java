package jadx.gui.utils;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Vector;

public class NLS {
	private static Vector<LangLocale> i18nLocales = new Vector<>();

	private static Map<LangLocale, Map<String, String>> i18nMessagesMap;

	// Use these two fields to avoid invoking Map.get() method twice.
	private static Map<String, String> localizedMessagesMap;
	private static Map<String, String> fallbackMessagesMap;

	private static LangLocale currentLocale;
	private static LangLocale localLocale;

	private static Charset javaCharset;
	private static Charset utf8Charset;

	static {
		javaCharset = Charset.forName("ISO-8859-1");
		utf8Charset = Charset.forName("UTF-8");
		i18nMessagesMap = new HashMap<>();

		localLocale = new LangLocale(Locale.getDefault());

		i18nLocales.add(new LangLocale("en", "US")); // As default language
		i18nLocales.add(new LangLocale("zh", "CN"));
		i18nLocales.add(new LangLocale("es", "ES"));

		i18nLocales.forEach(NLS::load);

		fallbackMessagesMap = i18nMessagesMap.get(i18nLocales.get(0));
		localizedMessagesMap = i18nMessagesMap.get(i18nLocales.get(0));
	}

	private NLS() {
	}

	private static void load(LangLocale locale) {
		ResourceBundle bundle = ResourceBundle.getBundle("i18n/Messages", locale.get());
		Map<String, String> resMap = new HashMap<>();

		for(String key : bundle.keySet()){
			resMap.put(key, new String(
							bundle.getString(key).getBytes(javaCharset),
							utf8Charset));
		}
		i18nMessagesMap.put(locale, resMap);
	}

	public static String str(String key) {
		if(localizedMessagesMap.containsKey(key)){
			return localizedMessagesMap.get(key);
		}
		return fallbackMessagesMap.get(key);// definitely exists
	}

	public static String str(String key, LangLocale locale) {
		if(i18nMessagesMap.get(locale).containsKey(key)){
			return i18nMessagesMap.get(locale).get(key);
		}
		return fallbackMessagesMap.get(key);// definitely exists
	}

	public static void setLocale(LangLocale locale) {
		if(i18nMessagesMap.containsKey(locale)){
			currentLocale = locale;
		} else {
			currentLocale = i18nLocales.get(0);
		}
		localizedMessagesMap = i18nMessagesMap.get(currentLocale);
	}

	public static Vector<LangLocale> getI18nLocales(){
		return i18nLocales;
	}

	public static LangLocale currentLocale() {
		return currentLocale;
	}

	public static LangLocale defaultLocale(){
		if(i18nMessagesMap.containsKey(localLocale)){
			return localLocale;
		} else {
			// fallback to english if unsupported
			return i18nLocales.get(0);
		}
	}
}
