package jadx.mcp.jadx;

import java.util.Arrays;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;

/**
 * Line index and paging for code/text results
 */
public final class CodeText {
	public static final int DEFAULT_MAX_CHARS = 60_000;
	public static final int MAX_CHARS_LIMIT = 500_000;

	private final String text;
	private final int[] lineStarts;

	public CodeText(String text) {
		this.text = text;
		this.lineStarts = buildLineStarts(text);
	}

	private static int[] buildLineStarts(String text) {
		int[] starts = new int[64];
		int count = 0;
		starts[count++] = 0;
		int len = text.length();
		for (int i = 0; i < len; i++) {
			if (text.charAt(i) == '\n' && i + 1 < len) {
				if (count == starts.length) {
					starts = Arrays.copyOf(starts, count * 2);
				}
				starts[count++] = i + 1;
			}
		}
		return Arrays.copyOf(starts, count);
	}

	public String getText() {
		return text;
	}

	public int getLinesCount() {
		return text.isEmpty() ? 0 : lineStarts.length;
	}

	/**
	 * @return 1-based line number for position
	 */
	public int lineForPos(int pos) {
		int idx = Arrays.binarySearch(lineStarts, pos);
		if (idx >= 0) {
			return idx + 1;
		}
		return -idx - 1;
	}

	public int lineStart(int line) {
		return lineStarts[Math.max(0, Math.min(line, lineStarts.length) - 1)];
	}

	/**
	 * End position of line (exclusive, without line separator)
	 */
	public int lineEnd(int line) {
		int start = lineStart(line);
		int end = lineFullEnd(Math.max(1, Math.min(line, lineStarts.length)));
		if (end > start && text.charAt(end - 1) == '\n') {
			end--;
		}
		if (end > start && text.charAt(end - 1) == '\r') {
			end--;
		}
		return end;
	}

	public String getLine(int line) {
		return text.substring(lineStart(line), lineEnd(line));
	}

	/**
	 * Page of lines from 'startLine' (1-based) limited by lines and chars count.
	 * Always includes at least one line (cut if longer than char limit).
	 */
	public JsonObject page(int startLine, @Nullable Integer maxLines, int maxChars) {
		return page(startLine, getLinesCount(), maxLines, maxChars);
	}

	/**
	 * Page inside lines range [startLine, lastLine]
	 */
	public JsonObject page(int startLine, int lastLine, @Nullable Integer maxLines, int maxChars) {
		int total = getLinesCount();
		int last = Math.min(lastLine, total);
		int first = Math.max(1, startLine);
		JsonObject obj = new JsonObject();
		if (first > last) {
			obj.addProperty("text", "");
			obj.addProperty("start_line", first);
			obj.addProperty("end_line", first - 1);
			obj.addProperty("total_lines", total);
			obj.addProperty("truncated", false);
			obj.addProperty("next_start_line", (Number) null);
			return obj;
		}
		int lineLimit = maxLines == null ? Integer.MAX_VALUE : Math.max(1, maxLines);
		int startPos = lineStart(first);
		int endLine = first;
		int endPos = lineFullEnd(first);
		boolean cut = false;
		if (endPos - startPos > maxChars) {
			endPos = startPos + maxChars;
			cut = true;
		} else {
			while (endLine < last && endLine - first + 1 < lineLimit) {
				int nextEnd = lineFullEnd(endLine + 1);
				if (nextEnd - startPos > maxChars) {
					break;
				}
				endLine++;
				endPos = nextEnd;
			}
		}
		boolean truncated = cut || endLine < last;
		obj.addProperty("text", text.substring(startPos, endPos));
		obj.addProperty("start_line", first);
		obj.addProperty("end_line", endLine);
		obj.addProperty("total_lines", total);
		obj.addProperty("truncated", truncated);
		if (truncated && endLine < total) {
			obj.addProperty("next_start_line", endLine + 1);
		} else {
			obj.addProperty("next_start_line", (Number) null);
		}
		if (cut) {
			obj.addProperty("line_cut", true);
		}
		return obj;
	}

	/**
	 * Line end including line separator
	 */
	private int lineFullEnd(int line) {
		return line < lineStarts.length ? lineStarts[line] : text.length();
	}

	public static int normalizeMaxChars(int maxChars) {
		if (maxChars <= 0) {
			return DEFAULT_MAX_CHARS;
		}
		return Math.min(maxChars, MAX_CHARS_LIMIT);
	}
}
