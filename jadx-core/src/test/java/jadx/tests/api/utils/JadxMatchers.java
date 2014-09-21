package jadx.tests.api.utils;

import jadx.core.codegen.CodeWriter;

import org.hamcrest.Matcher;

public class JadxMatchers {

	public static Matcher<String> countString(int count, String substring) {
		return new CountString(count, substring);
	}

	public static Matcher<String> containsOne(String substring) {
		return countString(1, substring);
	}

	public static Matcher<String> containsLines(String... lines) {
		StringBuilder sb = new StringBuilder();
		for (String line : lines) {
			sb.append(line).append(CodeWriter.NL);
		}
		return countString(1, sb.toString());
	}

	public static Matcher<String> containsLines(int commonIndent, String... lines) {
		String indent = TestUtils.indent(commonIndent);
		StringBuilder sb = new StringBuilder();
		for (String line : lines) {
			if (!line.isEmpty()) {
				sb.append(indent);
				sb.append(line);
			}
			sb.append(CodeWriter.NL);
		}
		return countString(1, sb.toString());
	}
}
