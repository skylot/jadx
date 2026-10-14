package jadx.mcp.jadx;

import org.jetbrains.annotations.Nullable;

/**
 * Find code ranges of classes and methods in decompiled code
 */
public final class CodeRanges {

	private CodeRanges() {
	}

	/**
	 * Lines range of node declaration including annotations and comments above it.
	 *
	 * @param defPos  declaration position from code metadata
	 * @param hasBody false for abstract/native methods (declaration ends with ';')
	 * @return [firstLine, lastLine] (1-based) or null if position unknown
	 */
	public static @Nullable int[] nodeLines(CodeText code, int defPos, boolean hasBody) {
		String text = code.getText();
		if (defPos < 0 || defPos >= text.length()) {
			return null;
		}
		int defLine = code.lineForPos(defPos);
		int firstLine = findDeclStartLine(code, defLine);
		int endPos = hasBody ? findBlockEnd(text, defPos) : findDeclEnd(text, defPos);
		int lastLine = endPos == -1 ? defLine : code.lineForPos(endPos);
		return new int[] { firstLine, Math.max(defLine, lastLine) };
	}

	/**
	 * Extend declaration start up to include annotations, line comments and whole block comments
	 * directly above it. Block comments are taken only completely: lines inside a block
	 * (stack traces, javadoc text) are not checked by prefix, block start is searched from its end.
	 */
	static int findDeclStartLine(CodeText code, int defLine) {
		int first = defLine;
		while (first > 1) {
			String prev = code.getLine(first - 1).trim();
			if (prev.startsWith("@") || prev.startsWith("//")) {
				first--;
				continue;
			}
			if (prev.endsWith("*/")) {
				int blockStart = findBlockCommentStart(code, first - 1);
				if (blockStart == -1) {
					break;
				}
				first = blockStart;
				continue;
			}
			break;
		}
		return first;
	}

	/**
	 * Find line where block comment ending at 'endLine' starts.
	 *
	 * @return line number or -1 if comment start not found or line has code before comment
	 */
	private static int findBlockCommentStart(CodeText code, int endLine) {
		for (int line = endLine; line >= 1; line--) {
			String str = code.getLine(line);
			// search opening from line end, '*/' on end line is not an opening
			int searchEnd = line == endLine ? str.lastIndexOf("*/") : str.length();
			int open = str.lastIndexOf("/*", Math.max(0, searchEnd - 1));
			if (open != -1 && (line != endLine || open < searchEnd)) {
				// comment must start the line, otherwise it belongs to code above
				return str.substring(0, open).trim().isEmpty() ? line : -1;
			}
		}
		return -1;
	}

	private static int findDeclEnd(String text, int pos) {
		int semicolon = text.indexOf(';', pos);
		return semicolon == -1 ? pos : semicolon;
	}

	/**
	 * Find position of closing brace of first block after 'pos'.
	 * Skips braces inside strings, chars and comments.
	 *
	 * @return position of '}' or -1
	 */
	public static int findBlockEnd(String text, int pos) {
		int len = text.length();
		int depth = 0;
		boolean started = false;
		int i = pos;
		while (i < len) {
			char c = text.charAt(i);
			char next = i + 1 < len ? text.charAt(i + 1) : '\0';
			if (c == '/' && next == '/') {
				int nl = text.indexOf('\n', i);
				if (nl == -1) {
					return -1;
				}
				i = nl + 1;
				continue;
			}
			if (c == '/' && next == '*') {
				int end = text.indexOf("*/", i + 2);
				if (end == -1) {
					return -1;
				}
				i = end + 2;
				continue;
			}
			if (c == '"') {
				if (text.startsWith("\"\"\"", i)) {
					int end = text.indexOf("\"\"\"", i + 3);
					if (end == -1) {
						return -1;
					}
					i = end + 3;
					continue;
				}
				i = skipQuoted(text, i, '"');
				continue;
			}
			if (c == '\'') {
				i = skipQuoted(text, i, '\'');
				continue;
			}
			if (c == ';' && !started) {
				// declaration without body
				return -1;
			}
			if (c == '{') {
				depth++;
				started = true;
			} else if (c == '}') {
				depth--;
				if (started && depth == 0) {
					return i;
				}
			}
			i++;
		}
		return -1;
	}

	private static int skipQuoted(String text, int start, char quote) {
		int len = text.length();
		int i = start + 1;
		while (i < len) {
			char c = text.charAt(i);
			if (c == '\\') {
				i += 2;
				continue;
			}
			if (c == quote || c == '\n') {
				return i + 1;
			}
			i++;
		}
		return len;
	}
}
