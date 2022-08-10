package jadx.core.deobf;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import jadx.core.utils.StringUtils;

import static jadx.core.utils.StringUtils.notEmpty;

public class NameMapper {

	public static final Pattern VALID_JAVA_IDENTIFIER = Pattern.compile(
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

	public static boolean isValidAndPrintable(String str) {
		return isValidIdentifier(str) && isAllCharsPrintable(str);
	}

	public static boolean isValidIdentifierStart(int codePoint) {
		return Character.isJavaIdentifierStart(codePoint);
	}

	public static boolean isValidIdentifierPart(int codePoint) {
		return Character.isJavaIdentifierPart(codePoint);
	}

	public static boolean isPrintableChar(char c) {
		return 32 <= c && c <= 126;
	}

	public static boolean isPrintableAsciiCodePoint(int c) {
		return 32 <= c && c <= 126;
	}

	public static boolean isPrintableCodePoint(int codePoint) {
		if (Character.isISOControl(codePoint)) {
			return false;
		}
		if (Character.isWhitespace(codePoint)) {
			// don't print whitespaces other than standard one
			return codePoint == ' ';
		}
		switch (Character.getType(codePoint)) {
			case Character.CONTROL:
			case Character.FORMAT:
			case Character.PRIVATE_USE:
			case Character.SURROGATE:
			case Character.UNASSIGNED:
				return false;
		}
		return true;
	}

	public static boolean isAllCharsPrintable(String str) {
		int len = str.length();
		int offset = 0;
		while (offset < len) {
			int codePoint = str.codePointAt(offset);
			if (!isPrintableAsciiCodePoint(codePoint)) {
				return false;
			}
			offset += Character.charCount(codePoint);
		}
		return true;
	}

	/**
	 * Return modified string with removed:
	 * <ul>
	 * <li>not printable chars (including unicode)
	 * <li>chars not valid for java identifier part
	 * </ul>
	 * Note: this 'middle' method must be used with prefixed string:
	 * <ul>
	 * <li>can leave invalid chars for java identifier start (i.e numbers)
	 * <li>result not checked for reserved words
	 * </ul>
	 */
	public static String removeInvalidCharsMiddle(String name) {
		if (isValidIdentifier(name) && isAllCharsPrintable(name)) {
			return name;
		}
		int len = name.length();
		StringBuilder sb = new StringBuilder(len);
		StringUtils.visitCodePoints(name, codePoint -> {
			if (isPrintableAsciiCodePoint(codePoint) && isValidIdentifierPart(codePoint)) {
				sb.appendCodePoint(codePoint);
			}
		});
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

	public static String removeNonPrintableCharacters(String name) {
		StringBuilder sb = new StringBuilder(name.length());
		StringUtils.visitCodePoints(name, codePoint -> {
			if (isPrintableAsciiCodePoint(codePoint)) {
				sb.appendCodePoint(codePoint);
			}
		});
		return sb.toString();
	}

	private NameMapper() {
	}
}
