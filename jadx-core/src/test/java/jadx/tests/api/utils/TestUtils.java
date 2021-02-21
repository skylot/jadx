package jadx.tests.api.utils;

import org.junit.jupiter.api.extension.ExtendWith;

import jadx.NotYetImplementedExtension;
import jadx.api.ICodeWriter;

@ExtendWith(NotYetImplementedExtension.class)
public class TestUtils {

	public static String indent() {
		return ICodeWriter.INDENT_STR;
	}

	public static String indent(int indent) {
		if (indent == 1) {
			return ICodeWriter.INDENT_STR;
		}
		StringBuilder sb = new StringBuilder(indent * ICodeWriter.INDENT_STR.length());
		for (int i = 0; i < indent; i++) {
			sb.append(ICodeWriter.INDENT_STR);
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
