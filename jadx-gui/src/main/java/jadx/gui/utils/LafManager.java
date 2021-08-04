package jadx.gui.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;

import ch.qos.logback.classic.Level;

import jadx.cli.LogHelper;
import jadx.gui.settings.JadxSettings;

public class LafManager {
	private static final Logger LOG = LoggerFactory.getLogger(LafManager.class);

	public static final String SYSTEM_THEME_NAME = "default";
	private static final Map<String, String> THEMES_MAP = initThemesMap();

	public static void init(JadxSettings settings) {
		if (setupLaf(getThemeClass(settings))) {
			return;
		}
		setupLaf(SYSTEM_THEME_NAME);
		settings.setLafTheme(SYSTEM_THEME_NAME);
		settings.sync();
	}

	public static void updateLaf(JadxSettings settings) {
		if (setupLaf(getThemeClass(settings))) {
			// update all components
			FlatLaf.updateUI();
			FlatAnimatedLafChange.hideSnapshotWithAnimation();
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
		for (FlatLaf flatLafTheme : collectFlatLafThemes()) {
			map.put(flatLafTheme.getName(), flatLafTheme.getClass().getName());
		}
		return map;
	}

	private static List<FlatLaf> collectFlatLafThemes() {
		LogHelper.setLevelForPackage("org.reflections", Level.WARN);
		Reflections reflections = new Reflections("com.formdev.flatlaf");
		Set<Class<? extends FlatLaf>> lafClasses = reflections.getSubTypesOf(FlatLaf.class);

		List<FlatLaf> themes = new ArrayList<>(lafClasses.size());
		for (Class<? extends FlatLaf> lafClass : lafClasses) {
			try {
				themes.add(lafClass.getDeclaredConstructor().newInstance());
			} catch (Exception e) {
				// some classes not themes, ignore them
				LOG.trace("Failed make instance for class: {}", lafClass.getName(), e);
			}
		}
		themes.sort(Comparator.comparing(LookAndFeel::getName));
		return themes;
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
