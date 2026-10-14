package jadx.mcp.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Typed access to validated tool arguments. Missing values are taken from schema 'default'.
 */
public final class ToolArgs {
	private final JsonObject args;
	private final JsonObject properties;

	public ToolArgs(JsonObject args, JsonObject inputSchema) {
		this.args = args;
		JsonElement props = inputSchema.get("properties");
		this.properties = props != null && props.isJsonObject() ? props.getAsJsonObject() : new JsonObject();
	}

	public boolean has(String name) {
		JsonElement value = args.get(name);
		return value != null && !value.isJsonNull();
	}

	private @Nullable JsonElement get(String name) {
		JsonElement value = args.get(name);
		if (value != null && !value.isJsonNull()) {
			return value;
		}
		JsonElement propSchema = properties.get(name);
		if (propSchema != null && propSchema.isJsonObject()) {
			JsonElement def = propSchema.getAsJsonObject().get("default");
			if (def != null && !def.isJsonNull()) {
				return def;
			}
		}
		return null;
	}

	public @Nullable String getString(String name) {
		JsonElement value = get(name);
		return value == null ? null : value.getAsString();
	}

	/**
	 * Return trimmed non-empty string or null
	 */
	public @Nullable String getOptString(String name) {
		String str = getString(name);
		if (str == null) {
			return null;
		}
		String trimmed = str.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	public String getRequiredString(String name) throws ToolException {
		String str = getOptString(name);
		if (str == null) {
			throw ToolException.invalidArg("Argument '" + name + "' is required and must be a non-empty string");
		}
		return str;
	}

	public int getInt(String name, int defaultValue) {
		JsonElement value = get(name);
		return value == null ? defaultValue : value.getAsInt();
	}

	public @Nullable Integer getOptInt(String name) {
		JsonElement value = get(name);
		return value == null ? null : value.getAsInt();
	}

	public boolean getBool(String name, boolean defaultValue) {
		JsonElement value = get(name);
		return value == null ? defaultValue : value.getAsBoolean();
	}

	public List<String> getStringList(String name) {
		JsonElement value = get(name);
		if (value == null || !value.isJsonArray()) {
			return Collections.emptyList();
		}
		List<String> list = new ArrayList<>();
		for (JsonElement el : value.getAsJsonArray()) {
			list.add(el.getAsString());
		}
		return list;
	}

	public JsonObject getRaw() {
		return args;
	}
}
