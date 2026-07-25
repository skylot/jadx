package jadx.gui.settings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.cli.config.IJadxConfig;
import jadx.cli.config.JadxConfigAdapter;
import jadx.gui.utils.LangLocale;
import jadx.gui.utils.NLS;

/**
 * Reads only {@code langLocale} from gui config.
 */
public final class GuiConfigLocale {
	private static final Logger LOG = LoggerFactory.getLogger(GuiConfigLocale.class);

	/**
	 * Truncated {@link JadxSettingsData} class with only locale field
	 */
	public static final class LocaleConfig implements IJadxConfig {
		public LangLocale langLocale;
	}

	public static void load() {
		LOG.debug("Loading locale config");
		JadxConfigAdapter<LocaleConfig> configAdapter = new JadxConfigAdapter<>(LocaleConfig.class, "gui");
		configAdapter.useConfigRef(""); // default config
		LocaleConfig localeConfig = configAdapter.load();
		if (localeConfig != null) {
			NLS.setLocale(localeConfig.langLocale);
		} else {
			LOG.warn("Can't load locale from config, using default");
			NLS.setLocale(NLS.defaultLocale());
		}
		LOG.debug("Loaded locale config: {}", NLS.currentLocale());
	}

	public static void checkConfig(JadxSettingsData settingsData) {
		LangLocale loadedLocale = settingsData.getLangLocale();
		if (!NLS.currentLocale().equals(loadedLocale)) {
			LOG.warn("Locale from non-default config loaded only partially!");
			NLS.setLocale(loadedLocale);
		}
	}

	private GuiConfigLocale() {
	}
}
