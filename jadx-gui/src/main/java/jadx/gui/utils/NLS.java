package jadx.gui.utils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Vector;

import jadx.core.utils.exceptions.JadxRuntimeException;

public class NLS {

	private static final Vector<LangLocale> LANG_LOCALES = new Vector<>();

	private static final Map<LangLocale, ResourceBundle> LANG_LOCALES_MAP = new HashMap<>();

	private static final ResourceBundle FALLBACK_MESSAGES_MAP;
	private static final LangLocale LOCAL_LOCALE;

	// Use these two fields to avoid invoking Map.get() method twice.
	private static ResourceBundle localizedMessagesMap;
	private static LangLocale currentLocale;

	static {
		LOCAL_LOCALE = new LangLocale(Locale.getDefault());

		LANG_LOCALES.add(new LangLocale("en", "US")); // As default language
		LANG_LOCALES.add(new LangLocale("zh", "CN"));
		LANG_LOCALES.add(new LangLocale("zh", "TW"));
		LANG_LOCALES.add(new LangLocale("es", "ES"));
		LANG_LOCALES.add(new LangLocale("de", "DE"));
		LANG_LOCALES.add(new LangLocale("ko", "KR"));
		LANG_LOCALES.add(new LangLocale("pt", "BR"));
		LANG_LOCALES.add(new LangLocale("ru", "RU"));

		LANG_LOCALES.forEach(NLS::load);

		LangLocale defLang = LANG_LOCALES.get(0);
		FALLBACK_MESSAGES_MAP = LANG_LOCALES_MAP.get(defLang);
		localizedMessagesMap = LANG_LOCALES_MAP.get(defLang);
	}

	private NLS() {
	}

	private static void load(LangLocale locale) {
		ResourceBundle bundle;
		ClassLoader classLoader = ClassLoader.getSystemClassLoader();
		String resName = String.format("i18n/Messages_%s.properties", locale.get());
		URL bundleUrl = classLoader.getResource(resName);
		if (bundleUrl == null) {
			throw new JadxRuntimeException("Locale resource not found: " + resName);
		}
		try (Reader reader = new InputStreamReader(bundleUrl.openStream(), StandardCharsets.UTF_8)) {
			bundle = new PropertyResourceBundle(reader);
		} catch (IOException e) {
			throw new JadxRuntimeException("Failed to load " + resName, e);
		}
		LANG_LOCALES_MAP.put(locale, bundle);
	}

	public static String str(String key) {
		try {
			return localizedMessagesMap.getString(key);
		} catch (Exception e) {
			return FALLBACK_MESSAGES_MAP.getString(key);
		}
	}

	public static String str(String key, Object... parameters) {
		return String.format(str(key), parameters);
	}

	public static String str(String key, LangLocale locale) {
		ResourceBundle bundle = LANG_LOCALES_MAP.get(locale);
		if (bundle != null) {
			try {
				return bundle.getString(key);
			} catch (MissingResourceException ignored) {
				// use fallback string
			}
		}
		return FALLBACK_MESSAGES_MAP.getString(key); // definitely exists
	}

	public static void setLocale(LangLocale locale) {
		if (LANG_LOCALES_MAP.containsKey(locale)) {
			currentLocale = locale;
		} else {
			currentLocale = LANG_LOCALES.get(0);
		}
		localizedMessagesMap = LANG_LOCALES_MAP.get(currentLocale);
	}

	public static Vector<LangLocale> getLangLocales() {
		return LANG_LOCALES;
	}

	public static LangLocale currentLocale() {
		return currentLocale;
	}

	public static LangLocale defaultLocale() {
		if (LANG_LOCALES_MAP.containsKey(LOCAL_LOCALE)) {
			return LOCAL_LOCALE;
		}
		// fallback to english if unsupported
		return LANG_LOCALES.get(0);
	}
}
