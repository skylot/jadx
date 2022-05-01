package jadx.api.impl;

import jadx.api.ICodeWriter;

public class CodeUtils {

	public static String getLineForPos(String code, int pos) {
		String newLine = ICodeWriter.NL;
		int start = code.lastIndexOf(newLine, pos);
		int end = code.indexOf(newLine, pos);
		int from = start == -1 ? 0 : start + newLine.length();
		int to = end == -1 ? code.length() : end;
		return code.substring(from, to);
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
