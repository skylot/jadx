package jadx.api.plugins.options.impl;

import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public class BaseOptionsParser {

	public boolean getBooleanOption(Map<String, String> options, String key, boolean defValue) {
		String val = options.get(key);
		if (val == null) {
			return defValue;
		}
		String valLower = val.toLowerCase(Locale.ROOT);
		if (valLower.equals("yes") || valLower.equals("true")) {
			return true;
		}
		if (valLower.equals("no") || valLower.equals("false")) {
			return false;
		}
		throw new IllegalArgumentException("Unknown value '" + val + "' for option '" + key + "'"
				+ ", expect: 'yes' or 'no'");
	}

	public <T> T getOption(Map<String, String> options, String key, Function<String, T> parse, T defValue) {
		String val = options.get(key);
		if (val == null) {
			return defValue;
		}
		return parse.apply(val);
	}
}
