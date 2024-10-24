package jadx.core.xmlgen;

import jadx.core.deobf.NameMapper;

import static jadx.core.deobf.NameMapper.*;

class ResNameUtils {

	private ResNameUtils() {
	}

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

	static String convertToJavaIdentifier(String name) {
		return name.replace('.', '_');
	}

	/**
	 * Determines whether the code point may be part of a resource name as the first character (aapt2 +
	 * R file gen).
	 */
	private static boolean isValidResourceNameStart(int codePoint, boolean allowNonPrintable) {
		return (allowNonPrintable || isPrintableAsciiCodePoint(codePoint))
				&& (isValidAapt2ResourceNameStart(codePoint) && isValidIdentifierStart(codePoint));
	}

	/**
	 * Determines whether the code point may be part of a resource name as other than the first
	 * character
	 * (aapt2 + R file gen).
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
