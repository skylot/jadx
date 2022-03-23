package jadx.core.utils.log;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Escape input from untrusted source before pass to logger.
 * Suggested by CodeQL: https://codeql.github.com/codeql-query-help/java/java-log-injection/
 */
public class LogUtils {

	private static final Pattern ALFA_NUMERIC = Pattern.compile("\\w*");

	public static String escape(String input) {
		if (input == null) {
			return "null";
		}
		if (ALFA_NUMERIC.matcher(input).matches()) {
			return input;
		}
		return input.replaceAll("\\W", ".");
	}

	public static String escape(byte[] input) {
		if (input == null) {
			return "null";
		}
		return escape(new String(input, StandardCharsets.UTF_8));
	}
}
