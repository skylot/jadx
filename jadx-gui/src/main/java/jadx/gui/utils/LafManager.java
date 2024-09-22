package jadx.gui.utils;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
// they misspelled it LOL
import com.jthemedetecor.OsThemeDetector;

import jadx.gui.settings.JadxSettings;

public class LafManager {
	private static final Logger LOG = LoggerFactory.getLogger(LafManager.class);

	public static final String SYSTEM_THEME_NAME = "default";
	public static String OS_DARK_THEME_NAME = FlatDarculaLaf.NAME;
	public static String OS_LIGHT_THEME_NAME = FlatLightLaf.NAME;
	public static final String INITIAL_THEME_NAME = OS_LIGHT_THEME_NAME;
	private static final OsThemeDetector detector = OsThemeDetector.getDetector();
	private static final Map<String, String> THEMES_MAP = initThemesMap();
		private static JadxSettings settings;
	private static void trySetThemeFromOSTheme(boolean dark) {
		try {
			// Ensure that settings object is initialized
			if (settings == null) {
				LOG.error("Settings object is not initialized");
				return;
			}

			// Ensure that themeName is correct
			String themeName = dark ? OS_DARK_THEME_NAME : OS_LIGHT_THEME_NAME;
			if (themeName == null || themeName.isEmpty()) {
				LOG.error("Theme name is not set");
				return;
			}

			// Set the look and feel
			UIManager.setLookAndFeel(themeName);
			settings.setLafTheme(SYSTEM_THEME_NAME);
			updateLaf(settings);
			LOG.info("Set LAF: {}", UIManager.getLookAndFeel().getName());
		} catch (Exception e) {
			// Log the exception to investigate the issue
			LOG.error("Failed to set OS theme", e);
		}
	}

	public static void init(JadxSettings settings) {
		if (setupLaf(getThemeClass(settings))) {
			return;
		}
		setupLaf(SYSTEM_THEME_NAME);
		trySetThemeFromOSTheme(detector.isDark());
		final OsThemeDetector detector = OsThemeDetector.getDetector();
		detector.registerListener(isDark -> SwingUtilities.invokeLater(() -> trySetThemeFromOSTheme(isDark)));
		settings.setLafTheme(SYSTEM_THEME_NAME);
		settings.sync();
		LafManager.settings = settings;
	}

	public static void updateLaf(JadxSettings settings) {
		if (setupLaf(getThemeClass(settings))) {
			// update all components
			FlatAnimatedLafChange.hideSnapshotWithAnimation();
			FlatLaf.updateUI();
		}
	}

	public static String[] getThemes() {
		return THEMES_MAP.keySet().toArray(new String[0]);
	}

	private static String getThemeClass(JadxSettings settings) {
		return THEMES_MAP.get(settings.getLafTheme());
	}

	private static boolean setupLaf(String themeClass) {
		if (SYSTEM_THEME_NAME.equals(themeClass)) {
			return applyLaf(UIManager.getSystemLookAndFeelClassName());
		}
		if (themeClass != null && !themeClass.isEmpty()) {
			return applyLaf(themeClass);
		}
		return false;
	}

	private static Map<String, String> initThemesMap() {
		Map<String, String> map = new LinkedHashMap<>();
		map.put(SYSTEM_THEME_NAME, SYSTEM_THEME_NAME);

		// default flatlaf themes
		map.put(FlatLightLaf.NAME, FlatLightLaf.class.getName());
		map.put(FlatDarkLaf.NAME, FlatDarkLaf.class.getName());
		map.put(FlatMacLightLaf.NAME, FlatMacLightLaf.class.getName());
		map.put(FlatMacDarkLaf.NAME, FlatMacDarkLaf.class.getName());
		map.put(FlatIntelliJLaf.NAME, FlatIntelliJLaf.class.getName());
		map.put(FlatDarculaLaf.NAME, FlatDarculaLaf.class.getName());

		// themes from flatlaf-intellij-themes
		for (FlatAllIJThemes.FlatIJLookAndFeelInfo themeInfo : FlatAllIJThemes.INFOS) {
			map.put(themeInfo.getName(), themeInfo.getClassName());
		}
		return map;
	}

	private static boolean applyLaf(String theme) {
		try {
			FlatAnimatedLafChange.showSnapshot();
			UIManager.setLookAndFeel(theme);
			return true;
		} catch (Exception e) {
			LOG.error("Failed to set laf to {}", theme, e);
			return false;
		}
	}
}
