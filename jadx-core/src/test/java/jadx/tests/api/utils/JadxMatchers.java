package jadx.tests.api.utils;

import org.hamcrest.Matcher;

import jadx.api.ICodeWriter;

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
			sb.append(line).append(ICodeWriter.NL);
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
			sb.append(ICodeWriter.NL);
		}
		return countString(1, sb.toString());
	}
}
