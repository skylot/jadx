package jadx.gui.settings;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.prefs.Preferences;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.JadxGUI;

public class JadxSettingsAdapter {

	private static final Logger LOG = LoggerFactory.getLogger(JadxSettingsAdapter.class);

	private static final String JADX_GUI_KEY = "jadx.gui.settings";

	private static final Preferences PREFS = Preferences.userNodeForPackage(JadxGUI.class);

	private static ExclusionStrategy EXCLUDE_FIELDS = new ExclusionStrategy() {
		@Override
		public boolean shouldSkipField(FieldAttributes f) {
			return JadxSettings.SKIP_FIELDS.contains(f.getName())
					|| f.hasModifier(Modifier.PUBLIC)
					|| f.hasModifier(Modifier.TRANSIENT);
		}

		@Override
		public boolean shouldSkipClass(Class<?> clazz) {
			return false;
		}
	};
	private static final GsonBuilder GSON_BUILDER = new GsonBuilder().setExclusionStrategies(EXCLUDE_FIELDS);
	private static final Gson GSON = GSON_BUILDER.create();

	private JadxSettingsAdapter() {
	}

	public static JadxSettings load() {
		try {
			String jsonSettings = PREFS.get(JADX_GUI_KEY, "");
			JadxSettings settings = fromString(jsonSettings);
			if (settings == null) {
				return new JadxSettings();
			}
			LOG.debug("Loaded settings: {}", makeString(settings));
			settings.fixOnLoad();
			return settings;
		} catch (Exception e) {
			LOG.error("Error load settings", e);
			return new JadxSettings();
		}
	}

	public static void store(JadxSettings settings) {
		try {
			String jsonSettings = makeString(settings);
			LOG.debug("Saving settings: {}", jsonSettings);
			PREFS.put(JADX_GUI_KEY, jsonSettings);
			PREFS.sync();
		} catch (Exception e) {
			LOG.error("Error store settings", e);
		}
	}

	public static JadxSettings fromString(String jsonSettings) {
		return GSON.fromJson(jsonSettings, JadxSettings.class);
	}

	public static String makeString(JadxSettings settings) {
		return GSON.toJson(settings);
	}

	public static void fill(JadxSettings settings, String jsonStr) {
		populate(GSON_BUILDER, jsonStr, JadxSettings.class, settings);
	}

	private static <T> void populate(GsonBuilder builder, String json, Class<T> type, final T into) {
		builder.registerTypeAdapter(type, new InstanceCreator<T>() {
			@Override
			public T createInstance(Type t) {
				return into;
			}
		}).create().fromJson(json, type);
	}
}
