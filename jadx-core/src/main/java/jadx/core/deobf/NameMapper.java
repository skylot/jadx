package jadx.core.deobf;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import static jadx.core.utils.StringUtils.notEmpty;

public class NameMapper {

	private static final Pattern VALID_JAVA_IDENTIFIER = Pattern.compile(
			"\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*");

	private static final Pattern VALID_JAVA_FULL_IDENTIFIER = Pattern.compile(
			"(" + VALID_JAVA_IDENTIFIER + "\\.)*" + VALID_JAVA_IDENTIFIER);

	private static final Set<String> RESERVED_NAMES = new HashSet<>(
			Arrays.asList(
					"abstract",
					"assert",
					"boolean",
					"break",
					"byte",
					"case",
					"catch",
					"char",
					"class",
					"const",
					"continue",
					"default",
					"do",
					"double",
					"else",
					"enum",
					"extends",
					"false",
					"final",
					"finally",
					"float",
					"for",
					"goto",
					"if",
					"implements",
					"import",
					"instanceof",
					"int",
					"interface",
					"long",
					"native",
					"new",
					"null",
					"package",
					"private",
					"protected",
					"public",
					"return",
					"short",
					"static",
					"strictfp",
					"super",
					"switch",
					"synchronized",
					"this",
					"throw",
					"throws",
					"transient",
					"true",
					"try",
					"void",
					"volatile",
					"while"));

	public static boolean isReserved(String str) {
		return RESERVED_NAMES.contains(str);
	}

	public static boolean isValidIdentifier(String str) {
		return notEmpty(str)
				&& !isReserved(str)
				&& VALID_JAVA_IDENTIFIER.matcher(str).matches();
	}

	public static boolean isValidFullIdentifier(String str) {
		return notEmpty(str)
				&& !isReserved(str)
				&& VALID_JAVA_FULL_IDENTIFIER.matcher(str).matches();
	}

	public static boolean isValidIdentifierStart(int codePoint) {
		return Character.isJavaIdentifierStart(codePoint);
	}

	public static boolean isValidIdentifierPart(int codePoint) {
		return Character.isJavaIdentifierPart(codePoint);
	}

	public static boolean isPrintableChar(int c) {
		return 32 <= c && c <= 126;
	}

	public static boolean isAllCharsPrintable(String str) {
		int len = str.length();
		for (int i = 0; i < len; i++) {
			if (!isPrintableChar(str.charAt(i))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Return modified string with removed:
	 * <p>
	 * <ul>
	 * <li>not printable chars (including unicode)
	 * <li>chars not valid for java identifier part
	 * </ul>
	 * <p>
	 * Note: this 'middle' method must be used with prefixed string:
	 * <p>
	 * <ul>
	 * <li>can leave invalid chars for java identifier start (i.e numbers)
	 * <li>result not checked for reserved words
	 * </ul>
	 * <p>
	 */
	public static String removeInvalidCharsMiddle(String name) {
		if (isValidIdentifier(name)) {
			return name;
		}
		int len = name.length();
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			int codePoint = name.codePointAt(i);
			if (isPrintableChar(codePoint) && isValidIdentifierPart(codePoint)) {
				sb.append((char) codePoint);
			}
		}
		return sb.toString();
	}

	/**
	 * Return string with removed invalid chars, see {@link #removeInvalidCharsMiddle}
	 * <p>
	 * Prepend prefix if first char is not valid as java identifier start char.
	 */
	public static String removeInvalidChars(String name, String prefix) {
		String result = removeInvalidCharsMiddle(name);
		if (!result.isEmpty()) {
			int codePoint = result.codePointAt(0);
			if (!isValidIdentifierStart(codePoint)) {
				return prefix + result;
			}
		}
		return result;
	}

	private NameMapper() {
	}
}
