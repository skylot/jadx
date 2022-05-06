package jadx.api.utils;

import jadx.api.ICodeWriter;

public class CodeUtils {

	public static String getLineForPos(String code, int pos) {
		int start = getLineStartForPos(code, pos);
		int end = getLineEndForPos(code, pos);
		return code.substring(start, end);
	}

	public static int getLineStartForPos(String code, int pos) {
		String newLine = ICodeWriter.NL;
		int start = code.lastIndexOf(newLine, pos);
		return start == -1 ? 0 : start + newLine.length();
	}

	public static int getLineEndForPos(String code, int pos) {
		int end = code.indexOf(ICodeWriter.NL, pos);
		return end == -1 ? code.length() : end;
	}

	public static int getLineNumForPos(String code, int pos) {
		String newLine = ICodeWriter.NL;
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
