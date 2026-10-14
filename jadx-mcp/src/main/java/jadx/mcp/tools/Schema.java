package jadx.mcp.tools;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Small builder for JSON Schema (2020-12 dialect, used by MCP 'inputSchema' and 'outputSchema')
 */
public final class Schema {

	private Schema() {
	}

	public static ObjectBuilder object() {
		return new ObjectBuilder();
	}

	public static JsonObject string(String description) {
		return typed("string", description);
	}

	public static JsonObject string(String description, @Nullable String defaultValue) {
		JsonObject s = string(description);
		if (defaultValue != null) {
			s.addProperty("default", defaultValue);
		}
		return s;
	}

	public static JsonObject nonEmptyString(String description) {
		JsonObject s = string(description);
		s.addProperty("minLength", 1);
		return s;
	}

	public static JsonObject nullableString(String description) {
		JsonObject s = new JsonObject();
		JsonArray types = new JsonArray();
		types.add("string");
		types.add("null");
		s.add("type", types);
		s.addProperty("description", description);
		return s;
	}

	public static JsonObject nullableInteger(String description) {
		JsonObject s = new JsonObject();
		JsonArray types = new JsonArray();
		types.add("integer");
		types.add("null");
		s.add("type", types);
		s.addProperty("description", description);
		return s;
	}

	public static JsonObject integer(String description) {
		return typed("integer", description);
	}

	public static JsonObject integer(String description, long min, long max, @Nullable Long defaultValue) {
		JsonObject s = integer(description);
		s.addProperty("minimum", min);
		s.addProperty("maximum", max);
		if (defaultValue != null) {
			s.addProperty("default", defaultValue);
		}
		return s;
	}

	public static JsonObject bool(String description) {
		return typed("boolean", description);
	}

	public static JsonObject bool(String description, boolean defaultValue) {
		JsonObject s = bool(description);
		s.addProperty("default", defaultValue);
		return s;
	}

	public static JsonObject enumOf(String description, @Nullable String defaultValue, String... values) {
		JsonObject s = string(description, defaultValue);
		JsonArray arr = new JsonArray();
		for (String value : values) {
			arr.add(value);
		}
		s.add("enum", arr);
		return s;
	}

	public static JsonObject array(JsonObject items, String description) {
		JsonObject s = typed("array", description);
		s.add("items", items);
		return s;
	}

	public static JsonObject stringArray(String description) {
		return array(typed("string", null), description);
	}

	public static JsonObject anyObject(String description) {
		return typed("object", description);
	}

	private static JsonObject typed(String type, @Nullable String description) {
		JsonObject s = new JsonObject();
		s.addProperty("type", type);
		if (description != null) {
			s.addProperty("description", description);
		}
		return s;
	}

	public static final class ObjectBuilder {
		private final Map<String, JsonObject> props = new LinkedHashMap<>();
		private final JsonArray required = new JsonArray();
		private @Nullable String description;
		private boolean strict = true;

		public ObjectBuilder required(String name, JsonObject schema) {
			props.put(name, schema);
			required.add(name);
			return this;
		}

		public ObjectBuilder optional(String name, JsonObject schema) {
			props.put(name, schema);
			return this;
		}

		public ObjectBuilder description(String desc) {
			this.description = desc;
			return this;
		}

		/**
		 * Allow additional properties (used for output schemas to allow future extensions)
		 */
		public ObjectBuilder open() {
			this.strict = false;
			return this;
		}

		public JsonObject build() {
			JsonObject s = new JsonObject();
			s.addProperty("type", "object");
			if (description != null) {
				s.addProperty("description", description);
			}
			JsonObject properties = new JsonObject();
			props.forEach(properties::add);
			s.add("properties", properties);
			if (!required.isEmpty()) {
				s.add("required", required);
			}
			if (strict) {
				s.addProperty("additionalProperties", false);
			}
			return s;
		}
	}
}
