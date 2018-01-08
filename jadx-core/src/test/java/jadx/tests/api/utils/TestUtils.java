package jadx.tests.api.utils;

import jadx.core.codegen.CodeWriter;

public class TestUtils {

	public static String indent(int indent) {
		if (indent == 1) {
			return CodeWriter.INDENT_STR;
		}
		StringBuilder sb = new StringBuilder(indent * CodeWriter.INDENT_STR.length());
		for (int i = 0; i < indent; i++) {
			sb.append(CodeWriter.INDENT_STR);
		}
		return sb.toString();
	}

	public static int count(String string, String substring) {
		int count = 0;
		int idx = 0;
		while ((idx = string.indexOf(substring, idx)) != -1) {
			idx++;
			count++;
		}
		return count;
	}

}
