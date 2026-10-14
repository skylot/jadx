package jadx.mcp.tools;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * Validator for the JSON Schema subset produced by {@link Schema}:
 * type (single or list), properties, required, additionalProperties (boolean or schema),
 * enum, minimum, maximum, minLength, maxLength, items, minItems, maxItems.
 */
public final class SchemaValidator {
	private static final int MAX_ERRORS = 20;

	private SchemaValidator() {
	}

	public static List<String> validate(JsonObject schema, JsonElement value) {
		List<String> errors = new ArrayList<>();
		check(schema, value, "$", errors);
		return errors;
	}

	private static void check(JsonObject schema, JsonElement value, String path, List<String> errors) {
		if (errors.size() >= MAX_ERRORS) {
			return;
		}
		JsonElement typeEl = schema.get("type");
		if (typeEl != null && !matchesType(typeEl, value)) {
			errors.add(path + ": expected " + typeEl + ", got " + describe(value));
			return;
		}
		JsonElement enumEl = schema.get("enum");
		if (enumEl != null && enumEl.isJsonArray() && !enumEl.getAsJsonArray().contains(value)) {
			errors.add(path + ": value " + value + " not one of " + enumEl);
		}
		if (value.isJsonPrimitive()) {
			checkPrimitive(schema, value.getAsJsonPrimitive(), path, errors);
		} else if (value.isJsonObject()) {
			checkObject(schema, value.getAsJsonObject(), path, errors);
		} else if (value.isJsonArray()) {
			checkArray(schema, value.getAsJsonArray(), path, errors);
		}
	}

	private static void checkPrimitive(JsonObject schema, JsonPrimitive value, String path, List<String> errors) {
		if (value.isNumber()) {
			BigDecimal num = value.getAsBigDecimal();
			JsonElement min = schema.get("minimum");
			if (min != null && num.compareTo(min.getAsBigDecimal()) < 0) {
				errors.add(path + ": value " + num + " is less than minimum " + min);
			}
			JsonElement max = schema.get("maximum");
			if (max != null && num.compareTo(max.getAsBigDecimal()) > 0) {
				errors.add(path + ": value " + num + " is greater than maximum " + max);
			}
		} else if (value.isString()) {
			int len = value.getAsString().codePointCount(0, value.getAsString().length());
			JsonElement minLen = schema.get("minLength");
			if (minLen != null && len < minLen.getAsInt()) {
				errors.add(path + ": string is shorter than " + minLen.getAsInt());
			}
			JsonElement maxLen = schema.get("maxLength");
			if (maxLen != null && len > maxLen.getAsInt()) {
				errors.add(path + ": string is longer than " + maxLen.getAsInt());
			}
		}
	}

	private static void checkObject(JsonObject schema, JsonObject value, String path, List<String> errors) {
		JsonObject props = schema.has("properties") ? schema.getAsJsonObject("properties") : new JsonObject();
		JsonElement requiredEl = schema.get("required");
		if (requiredEl != null && requiredEl.isJsonArray()) {
			for (JsonElement req : requiredEl.getAsJsonArray()) {
				String name = req.getAsString();
				if (!value.has(name)) {
					errors.add(path + ": missing required property '" + name + "'");
				}
			}
		}
		JsonElement additional = schema.get("additionalProperties");
		for (Map.Entry<String, JsonElement> entry : value.entrySet()) {
			String name = entry.getKey();
			String propPath = path + '.' + name;
			JsonElement propSchema = props.get(name);
			if (propSchema != null && propSchema.isJsonObject()) {
				check(propSchema.getAsJsonObject(), entry.getValue(), propPath, errors);
			} else if (additional != null) {
				if (additional.isJsonPrimitive() && !additional.getAsBoolean()) {
					errors.add(path + ": unknown property '" + name + "'"
							+ (props.size() == 0 ? "" : ", allowed: " + props.keySet()));
				} else if (additional.isJsonObject()) {
					check(additional.getAsJsonObject(), entry.getValue(), propPath, errors);
				}
			}
		}
	}

	private static void checkArray(JsonObject schema, JsonArray value, String path, List<String> errors) {
		JsonElement minItems = schema.get("minItems");
		if (minItems != null && value.size() < minItems.getAsInt()) {
			errors.add(path + ": array has less than " + minItems.getAsInt() + " items");
		}
		JsonElement maxItems = schema.get("maxItems");
		if (maxItems != null && value.size() > maxItems.getAsInt()) {
			errors.add(path + ": array has more than " + maxItems.getAsInt() + " items");
		}
		JsonElement items = schema.get("items");
		if (items != null && items.isJsonObject()) {
			for (int i = 0; i < value.size(); i++) {
				check(items.getAsJsonObject(), value.get(i), path + '[' + i + ']', errors);
			}
		}
	}

	private static boolean matchesType(JsonElement typeEl, JsonElement value) {
		if (typeEl.isJsonArray()) {
			for (JsonElement t : typeEl.getAsJsonArray()) {
				if (matchesType(t.getAsString(), value)) {
					return true;
				}
			}
			return false;
		}
		return matchesType(typeEl.getAsString(), value);
	}

	private static boolean matchesType(String type, JsonElement value) {
		switch (type) {
			case "null":
				return value.isJsonNull();
			case "object":
				return value.isJsonObject();
			case "array":
				return value.isJsonArray();
			case "string":
				return value.isJsonPrimitive() && value.getAsJsonPrimitive().isString();
			case "boolean":
				return value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean();
			case "number":
				return value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber();
			case "integer":
				if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
					BigDecimal num = value.getAsBigDecimal();
					return num.stripTrailingZeros().scale() <= 0;
				}
				return false;
			default:
				return true;
		}
	}

	private static String describe(JsonElement value) {
		if (value.isJsonNull()) {
			return "null";
		}
		if (value.isJsonObject()) {
			return "object";
		}
		if (value.isJsonArray()) {
			return "array";
		}
		JsonPrimitive p = value.getAsJsonPrimitive();
		if (p.isString()) {
			return "string";
		}
		if (p.isBoolean()) {
			return "boolean";
		}
		return "number";
	}
}
