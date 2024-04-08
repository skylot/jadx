package jadx.api.utils;

public class CodeUtils {

	public static String getLineForPos(String code, int pos) {
		int start = getLineStartForPos(code, pos);
		int end = getLineEndForPos(code, pos);
		return code.substring(start, end);
	}

	public static int getLineStartForPos(String code, int pos) {
		int start = getNewLinePosBefore(code, pos);
		return start == -1 ? 0 : start + 1;
	}

	public static int getLineEndForPos(String code, int pos) {
		int end = getNewLinePosAfter(code, pos);
		return end == -1 ? code.length() : end;
	}

	public static int getNewLinePosAfter(String code, int startPos) {
		int pos = code.indexOf('\n', startPos);
		if (pos != -1) {
			// check for '\r\n'
			int prev = pos - 1;
			if (code.charAt(prev) == '\r') {
				return prev;
			}
		}
		return pos;
	}

	public static int getNewLinePosBefore(String code, int startPos) {
		return code.lastIndexOf('\n', startPos);
	}

	public static int getLineNumForPos(String code, int pos, String newLine) {
		int newLineLen = newLine.length();
		int line = 1;
		int prev = 0;
		while (true) {
			int next = code.indexOf(newLine, prev);
			if (next >= pos) {
				return line;
			}
			prev = next + newLineLen;
			line++;
		}
	}
}
