package jadx.core.utils;

public class StringUtils {

	private StringUtils() {
	}

	public static String unescapeString(String str) {
		int len = str.length();
		StringBuilder res = new StringBuilder();

		for (int i = 0; i < len; i++) {
			int c = str.charAt(i) & 0xFFFF;
			processChar(c, res);
		}
		return '"' + res.toString() + '"';
	}

	public static String unescapeChar(char ch) {
		if (ch == '\'') {
			return "'\\\''";
		}
		StringBuilder res = new StringBuilder();
		processChar(ch, res);
		return '\'' + res.toString() + '\'';
	}

	private static void processChar(int c, StringBuilder res) {
		switch (c) {
			case '\n': res.append("\\n"); break;
			case '\r': res.append("\\r"); break;
			case '\t': res.append("\\t"); break;
			case '\b': res.append("\\b"); break;
			case '\f': res.append("\\f"); break;
			case '\'': res.append('\''); break;
			case '"': res.append("\\\""); break;
			case '\\': res.append("\\\\"); break;

			default:
				if (32 <= c && c <= 126) {
					res.append((char) c);
				} else {
					res.append("\\u").append(String.format("%04x", c));
				}
				break;
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
			switch (c) {
				case '&': sb.append("&amp;"); break;
				case '<': sb.append("&lt;"); break;
				case '>': sb.append("&gt;"); break;
				case '"': sb.append("&quot;"); break;
				case '\'': sb.append("&apos;"); break;
				default:
					sb.append(c);
					break;
			}
		}
		return sb.toString();
	}

	public static String escapeResValue(String str) {
		int len = str.length();
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < len; i++) {
			char c = str.charAt(i);
			switch (c) {
				case '&': sb.append("&amp;"); break;
				case '<': sb.append("&lt;"); break;
				case '>': sb.append("&gt;"); break;
				case '"': sb.append("&quot;"); break;
				case '\'': sb.append("&apos;"); break;

				case '\n': sb.append("\\n"); break;
				case '\r': sb.append("\\r"); break;
				case '\t': sb.append("\\t"); break;
				case '\b': sb.append("\\b"); break;
				case '\f': sb.append("\\f"); break;
				default:
					sb.append(c);
					break;
			}
		}
		return sb.toString();
	}
}
