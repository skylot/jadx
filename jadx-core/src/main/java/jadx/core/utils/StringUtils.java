package jadx.core.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.jetbrains.annotations.Nullable;

import jadx.api.JadxArgs;
import jadx.core.deobf.NameMapper;

public class StringUtils {
	private static final StringUtils DEFAULT_INSTANCE = new StringUtils(new JadxArgs());
	private static final String WHITES = " \t\r\n\f\b";
	private static final String WORD_SEPARATORS = WHITES + "(\")<,>{}=+-*/|[]\\:;'.`~!#^&";

	public static StringUtils getInstance() {
		return DEFAULT_INSTANCE;
	}

	private final boolean escapeUnicode;

	public StringUtils(JadxArgs args) {
		this.escapeUnicode = args.isEscapeUnicode();
	}

	public String unescapeString(String str) {
		int len = str.length();
		if (len == 0) {
			return "\"\"";
		}
		StringBuilder res = new StringBuilder();
		res.append('"');
		for (int i = 0; i < len; i++) {
			int c = str.charAt(i) & 0xFFFF;
			processCharInsideString(c, res);
		}
		res.append('"');
		return res.toString();
	}

	private void processCharInsideString(int c, StringBuilder res) {
		String str = getSpecialStringForChar(c);
		if (str != null) {
			res.append(str);
			return;
		}
		if (c < 32 || c >= 127 && escapeUnicode) {
			res.append("\\u").append(String.format("%04x", c));
		} else {
			res.append((char) c);
		}
	}

	/**
	 * Represent single char best way possible
	 */
	public String unescapeChar(int c, boolean explicitCast) {
		if (c == '\'') {
			return "'\\''";
		}
		String str = getSpecialStringForChar(c);
		if (str != null) {
			return '\'' + str + '\'';
		}
		if (c >= 127 && escapeUnicode) {
			return String.format("'\\u%04x'", c);
		}
		if (NameMapper.isPrintableChar(c)) {
			return "'" + (char) c + '\'';
		}
		if (explicitCast) {
			return "(char) " + c;
		}
		return String.valueOf(c);
	}

	public String unescapeChar(char ch) {
		return unescapeChar(ch, false);
	}

	@Nullable
	private String getSpecialStringForChar(int c) {
		switch (c) {
			case '\n':
				return "\\n";
			case '\r':
				return "\\r";
			case '\t':
				return "\\t";
			case '\b':
				return "\\b";
			case '\f':
				return "\\f";
			case '\'':
				return "'";
			case '"':
				return "\\\"";
			case '\\':
				return "\\\\";

			default:
				return null;
		}
	}

	public static String escape(String str) {
		int len = str.length();
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			char c = str.charAt(i);
			switch (c) {
				case '.':
				case '/':
				case ';':
				case '$':
				case ' ':
				case ',':
				case '<':
					sb.append('_');
					break;

				case '[':
					sb.append('A');
					break;

				case ']':
				case '>':
				case '?':
				case '*':
					break;

				default:
					sb.append(c);
					break;
			}
		}
		return sb.toString();
	}

	public static String escapeXML(String str) {
		int len = str.length();
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			char c = str.charAt(i);
			String replace = escapeXmlChar(c);
			if (replace != null) {
				sb.append(replace);
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	public static String escapeResValue(String str) {
		int len = str.length();
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			char c = str.charAt(i);
			commonEscapeAndAppend(sb, c);
		}
		return sb.toString();
	}

	public static String escapeResStrValue(String str) {
		int len = str.length();
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			char c = str.charAt(i);
			switch (c) {
				case '"':
					sb.append("\\\"");
					break;
				case '\'':
					sb.append("\\'");
					break;
				default:
					commonEscapeAndAppend(sb, c);
					break;
			}
		}
		return sb.toString();
	}

	private static String escapeXmlChar(char c) {
		if (c >= 0 && c <= 0x1F) {
			return "\\" + (int) c;
		}
		switch (c) {
			case '&':
				return "&amp;";
			case '<':
				return "&lt;";
			case '>':
				return "&gt;";
			case '"':
				return "&quot;";
			case '\'':
				return "&apos;";
			case '\\':
				return "\\\\";
			default:
				return null;
		}
	}

	private static String escapeWhiteSpaceChar(char c) {
		switch (c) {
			case '\n':
				return "\\n";
			case '\r':
				return "\\r";
			case '\t':
				return "\\t";
			case '\b':
				return "\\b";
			case '\f':
				return "\\f";
			default:
				return null;
		}
	}

	private static void commonEscapeAndAppend(StringBuilder sb, char c) {
		String replace = escapeWhiteSpaceChar(c);
		if (replace == null) {
			replace = escapeXmlChar(c);
		}
		if (replace != null) {
			sb.append(replace);
		} else {
			sb.append(c);
		}
	}

	public static boolean notEmpty(String str) {
		return str != null && !str.isEmpty();
	}

	public static boolean isEmpty(String str) {
		return str == null || str.isEmpty();
	}

	public static boolean notBlank(String str) {
		return notEmpty(str) && !str.trim().isEmpty();
	}

	public static int countMatches(String str, String subStr) {
		if (str == null || str.isEmpty() || subStr == null || subStr.isEmpty()) {
			return 0;
		}
		int subStrLen = subStr.length();
		int count = 0;
		int idx = 0;
		while ((idx = str.indexOf(subStr, idx)) != -1) {
			count++;
			idx += subStrLen;
		}
		return count;
	}

	/**
	 * returns how many lines does it have between start to pos in content.
	 */
	public static int countLinesByPos(String content, int pos, int start) {
		if (start >= pos) {
			return 0;
		}
		int count = 0;
		int tempPos = start;
		do {
			tempPos = content.indexOf("\n", tempPos);
			if (tempPos == -1) {
				break;
			}
			if (tempPos >= pos) {
				break;
			}
			count += 1;
			tempPos += 1;
		} while (tempPos < content.length());
		return count;
	}

	/**
	 * returns lines that contain pos to end if end is not -1.
	 */
	public static String getLine(String content, int pos, int end) {
		if (pos >= content.length()) {
			return "";
		}
		if (end != -1) {
			if (end > content.length()) {
				end = content.length() - 1;
			}
		} else {
			end = pos + 1;
		}
		// get to line head
		int headPos = content.lastIndexOf("\n", pos);
		if (headPos == -1) {
			headPos = 0;
		}
		// get to line end
		int endPos = content.indexOf("\n", end);
		if (endPos == -1) {
			endPos = content.length();
		}
		return content.substring(headPos, endPos);
	}

	public static boolean isWhite(char chr) {
		return WHITES.indexOf(chr) != -1;
	}

	public static boolean isWordSeparator(char chr) {
		return WORD_SEPARATORS.indexOf(chr) != -1;

	}

	public static String getDateText() {
		return new SimpleDateFormat("HH:mm:ss").format(new Date());
	}
}
