package jadx.gui.settings;

import java.awt.Rectangle;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonObject;

import jadx.gui.utils.PathTypeAdapter;
import jadx.gui.utils.RectangleTypeAdapter;

public class JadxSettingsAdapter {

	private static final Logger LOG = LoggerFactory.getLogger(JadxSettingsAdapter.class);

	private static final JadxSettingsStorage STORAGE = new JadxSettingsStorage();

	private static final ExclusionStrategy EXCLUDE_FIELDS = new ExclusionStrategy() {
		@Override
		public boolean shouldSkipField(FieldAttributes f) {
			return JadxSettings.SKIP_FIELDS.contains(f.getName())
					|| f.hasModifier(Modifier.PUBLIC)
					|| f.hasModifier(Modifier.TRANSIENT)
					|| f.hasModifier(Modifier.STATIC)
					|| (f.getAnnotation(GsonExclude.class) != null);
		}

		@Override
		public boolean shouldSkipClass(Class<?> clazz) {
			return false;
		}
	};
	private static final GsonBuilder GSON_BUILDER = new GsonBuilder()
			.setExclusionStrategies(EXCLUDE_FIELDS)
			.registerTypeHierarchyAdapter(Path.class, PathTypeAdapter.singleton())
			.registerTypeHierarchyAdapter(Rectangle.class, RectangleTypeAdapter.singleton())
			.setPrettyPrinting();
	private static final Gson GSON = GSON_BUILDER.create();

	private JadxSettingsAdapter() {
	}

	public static JadxSettings load() {
		try {
			JadxSettings settings = fromString(STORAGE.load());
			if (settings == null) {
				LOG.debug("Created new settings.");
				settings = JadxSettings.makeDefault();
			} else {
				settings.fixOnLoad();
			}
			return settings;
		} catch (Exception e) {
			LOG.error("Error load settings. Settings will reset", e);
			return new JadxSettings();
		}
	}

	public static void store(JadxSettings settings) {
		try {
			STORAGE.save(makeString(settings));
		} catch (Exception e) {
			LOG.error("Error store settings", e);
		}
	}

	public static JadxSettings fromString(String jsonSettings) {
		if (jsonSettings == null) {
			return null;
		}
		return GSON.fromJson(jsonSettings, JadxSettings.class);
	}

	public static String makeString(JadxSettings settings) {
		return GSON.toJson(settings);
	}

	public static JsonObject makeJsonObject(JadxSettings settings) {
		return GSON.toJsonTree(settings).getAsJsonObject();
	}

	public static void fill(JadxSettings settings, String jsonStr) {
		populate(GSON_BUILDER, jsonStr, JadxSettings.class, settings);
	}

	private static <T> void populate(GsonBuilder builder, String json, Class<T> type, final T into) {
		builder.registerTypeAdapter(type, (InstanceCreator<T>) t -> into)
				.create()
				.fromJson(json, type);
	}

	/**
	 * Annotation for specifying fields that should not be be saved/loaded
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public @interface GsonExclude {
	}
}
