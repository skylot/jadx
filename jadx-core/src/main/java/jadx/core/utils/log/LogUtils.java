package jadx.core.utils.log;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Escape input from untrusted source before pass to logger.
 * Suggested by CodeQL: https://codeql.github.com/codeql-query-help/java/java-log-injection/
 */
public class LogUtils {

	/**
	 * We replace everything except alphanumeric characters, underscore, dots, colon, semicolon, comma,
	 * spaces, minus
	 */
	private static final Pattern REPLACE_PATTERN = Pattern.compile("[^\\w\\.:;, -]");

	public static String escape(String input) {
		if (input == null) {
			return "null";
		}

		return REPLACE_PATTERN.matcher(input).replaceAll(".");
	}

	public static String escape(byte[] input) {
		if (input == null) {
			return "null";
		}
		return escape(new String(input, StandardCharsets.UTF_8));
	}
}
