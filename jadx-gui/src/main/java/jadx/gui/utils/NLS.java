package jadx.gui.utils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.jetbrains.annotations.NotNull;

public class NLS {

	private static Vector<LangLocale> i18nLocales = new Vector<>();

	private static Map<LangLocale, ResourceBundle> i18nMessagesMap = new HashMap<>();

	// Use these two fields to avoid invoking Map.get() method twice.
	private static ResourceBundle localizedMessagesMap;
	private static ResourceBundle fallbackMessagesMap;

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
		ResourceBundle bundle;
		ClassLoader classLoader = ClassLoader.getSystemClassLoader();
		String resName = String.format("i18n/Messages_%s.properties", locale.get());
		URL bundleUrl = classLoader.getResource(resName);
		try (Reader reader = new InputStreamReader(bundleUrl.openStream(), StandardCharsets.UTF_8)) {
			bundle = new PropertyResourceBundle(reader);
		} catch (IOException e) {
			throw new RuntimeException("Failed to load " + resName, e);
		}
		i18nMessagesMap.put(locale, bundle);
	}

	public static String str(String key) {
		try {
			return localizedMessagesMap.getString(key);
		} catch (MissingResourceException e) {
			return fallbackMessagesMap.getString(key); // definitely exists
		}
	}

	public static String str(String key, LangLocale locale) {
		ResourceBundle bundle = i18nMessagesMap.get(locale);
		if (bundle != null) {
			try {
				return bundle.getString(key);
			} catch (MissingResourceException e) {
			}
		}
		return fallbackMessagesMap.getString(key); // definitely exists
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
