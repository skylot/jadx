package jadx.core.xmlgen;

import jadx.core.deobf.NameMapper;

import static jadx.core.deobf.NameMapper.*;

class ResNameUtils {

	private ResNameUtils() {
	}

	/**
	 * Sanitizes the name so that it can be used as a resource name.
	 * By resource name is meant that:
	 * <ul>
	 * <li>It can be used by aapt2 as a resource entry name.
	 * <li>It can be converted to a valid R class field name.
	 * </ul>
	 * <p>
	 * If the {@code name} is already a valid resource name, the method returns it unchanged.
	 * If not, the method creates a valid resource name based on {@code name}, appends the
	 * {@code postfix}, and returns the result.
	 */
	static String sanitizeAsResourceName(String name, String postfix, boolean allowNonPrintable) {
		if (name.isEmpty()) {
			return postfix;
		}

		final StringBuilder sb = new StringBuilder(name.length() + 1);
		boolean nameChanged = false;

		int cp = name.codePointAt(0);
		if (isValidResourceNameStart(cp, allowNonPrintable)) {
			sb.appendCodePoint(cp);
		} else {
			sb.append('_');
			nameChanged = true;

			if (isValidResourceNamePart(cp, allowNonPrintable)) {
				sb.appendCodePoint(cp);
			}
		}

		for (int i = Character.charCount(cp); i < name.length(); i += Character.charCount(cp)) {
			cp = name.codePointAt(i);
			if (isValidResourceNamePart(cp, allowNonPrintable)) {
				sb.appendCodePoint(cp);
			} else {
				sb.append('_');
				nameChanged = true;
			}
		}

		final String sanitizedName = sb.toString();
		if (NameMapper.isReserved(sanitizedName)) {
			nameChanged = true;
		}

		return nameChanged
				? sanitizedName + postfix
				: sanitizedName;
	}

	/**
	 * Converts the resource name to a field name of the R class.
	 */
	static String convertToRFieldName(String resourceName) {
		return resourceName.replace('.', '_');
	}

	/**
	 * Determines whether the code point may be part of a resource name as the first character (aapt2 +
	 * R class gen).
	 */
	private static boolean isValidResourceNameStart(int codePoint, boolean allowNonPrintable) {
		return (allowNonPrintable || isPrintableAsciiCodePoint(codePoint))
				&& (isValidAapt2ResourceNameStart(codePoint) && isValidIdentifierStart(codePoint));
	}

	/**
	 * Determines whether the code point may be part of a resource name as other than the first
	 * character
	 * (aapt2 + R class gen).
	 */
	private static boolean isValidResourceNamePart(int codePoint, boolean allowNonPrintable) {
		return (allowNonPrintable || isPrintableAsciiCodePoint(codePoint))
				&& ((isValidAapt2ResourceNamePart(codePoint) && isValidIdentifierPart(codePoint)) || codePoint == '.');
	}

	/**
	 * Determines whether the code point may be part of a resource name as the first character (aapt2).
	 * <p>
	 * Source: <a href=
	 * "https://cs.android.com/android/platform/superproject/+/android15-release:frameworks/base/tools/aapt2/text/Unicode.cpp;l=112">aapt2/text/Unicode.cpp#L112</a>
	 */
	private static boolean isValidAapt2ResourceNameStart(int codePoint) {
		return isXidStart(codePoint) || codePoint == '_';
	}

	/**
	 * Determines whether the code point may be part of a resource name as other than the first
	 * character (aapt2).
	 * <p>
	 * Source: <a href=
	 * "https://cs.android.com/android/platform/superproject/+/android15-release:frameworks/base/tools/aapt2/text/Unicode.cpp;l=118">aapt2/text/Unicode.cpp#L118</a>
	 */
	private static boolean isValidAapt2ResourceNamePart(int codePoint) {
		return isXidContinue(codePoint) || codePoint == '.' || codePoint == '-';
	}

	private static boolean isXidStart(int codePoint) {
		// TODO: Need to implement a full check if the code point is XID_Start.
		return codePoint < 0x0370 && Character.isUnicodeIdentifierStart(codePoint);
	}

	private static boolean isXidContinue(int codePoint) {
		// TODO: Need to implement a full check if the code point is XID_Continue.
		return codePoint < 0x0370
				&& (Character.isUnicodeIdentifierPart(codePoint) && !Character.isIdentifierIgnorable(codePoint));
	}
}
