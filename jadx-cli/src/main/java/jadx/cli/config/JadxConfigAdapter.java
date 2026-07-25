package jadx.cli.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import jadx.commons.app.JadxCommonFiles;
import jadx.core.utils.GsonUtils;
import jadx.core.utils.exceptions.JadxArgsValidateException;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class JadxConfigAdapter<T extends IJadxConfig> {
	private static final ExclusionStrategy GSON_EXCLUSION_STRATEGY = new ExclusionStrategy() {
		@Override
		public boolean shouldSkipField(FieldAttributes f) {
			return f.getAnnotation(JadxConfigExclude.class) != null;
		}

		@Override
		public boolean shouldSkipClass(Class<?> clazz) {
			return false;
		}
	};

	private final Class<T> configCls;
	private final String defaultConfigFileName;
	private final Gson gson;

	private Path configPath;

	public JadxConfigAdapter(Class<T> configCls, String defaultConfigName) {
		this(configCls, defaultConfigName, gsonBuilder -> {
		});
	}

	public JadxConfigAdapter(Class<T> configCls, String defaultConfigName, Consumer<GsonBuilder> applyGsonOptions) {
		this.configCls = configCls;
		this.defaultConfigFileName = defaultConfigName + ".json";
		GsonBuilder gsonBuilder = GsonUtils.defaultGsonBuilder();
		gsonBuilder.setExclusionStrategies(GSON_EXCLUSION_STRATEGY);
		applyGsonOptions.accept(gsonBuilder);
		this.gson = gsonBuilder.create();
	}

	public void useConfigRef(String configRef) {
		this.configPath = resolveConfigRef(configRef);
	}

	public Path getConfigPath() {
		return configPath;
	}

	public String getDefaultConfigFileName() {
		return defaultConfigFileName;
	}

	public @Nullable T load() {
		if (!Files.isRegularFile(configPath)) {
			// file not found
			return null;
		}
		try (JsonReader reader = gson.newJsonReader(Files.newBufferedReader(configPath))) {
			return gson.fromJson(reader, configCls);
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to load config file: " + configPath, e);
		}
	}

	public void save(T configObject) {
		try {
			String jsonStr = gson.toJson(configObject, configCls);
			// don't use stream writer here because serialization errors will corrupt config
			Files.writeString(configPath, jsonStr);
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to save config file: " + configPath, e);
		}
	}

	public String objectToJsonString(T configObject) {
		return gson.toJson(configObject, configCls);
	}

	public T jsonStringToObject(String jsonStr) {
		return gson.fromJson(jsonStr, configCls);
	}

	private Path resolveConfigRef(String configRef) {
		if (configRef == null || configRef.isEmpty()) {
			// use default config file
			return JadxCommonFiles.getConfigDir().resolve(defaultConfigFileName);
		}
		if (configRef.contains("/") || configRef.contains("\\")) {
			if (!configRef.toLowerCase().endsWith(".json")) {
				throw new JadxArgsValidateException("Config file extension should be '.json'");
			}
			return Path.of(configRef);
		}
		// treat as a short name
		return JadxCommonFiles.getConfigDir().resolve(configRef + ".json");
	}
}
