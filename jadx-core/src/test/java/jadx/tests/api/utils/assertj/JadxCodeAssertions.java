package jadx.tests.api.utils.assertj;

import org.assertj.core.api.AbstractStringAssert;

import jadx.api.ICodeWriter;
import jadx.tests.api.utils.TestUtils;

public class JadxCodeAssertions extends AbstractStringAssert<JadxCodeAssertions> {
	public JadxCodeAssertions(String code) {
		super(code, JadxCodeAssertions.class);
	}

	public JadxCodeAssertions containsOne(String substring) {
		return countString(1, substring);
	}

	public JadxCodeAssertions countString(int count, String substring) {
		isNotNull();
		int actualCount = TestUtils.count(actual, substring);
		if (actualCount != count) {
			failWithMessage("Expected a substring <%s> count <%d> but was <%d>", substring, count, actualCount);
		}
		return this;
	}

	public JadxCodeAssertions notContainsLine(int indent, String line) {
		return countLine(0, indent, line);
	}

	public JadxCodeAssertions containsLine(int indent, String line) {
		return countLine(1, indent, line);
	}

	private JadxCodeAssertions countLine(int count, int indent, String line) {
		String indentStr = TestUtils.indent(indent);
		return countString(count, indentStr + line);
	}

	public JadxCodeAssertions containsLines(String... lines) {
		return containsLines(0, lines);
	}

	public JadxCodeAssertions containsLines(int commonIndent, String... lines) {
		if (lines.length == 1) {
			return containsLine(commonIndent, lines[0]);
		}
		String indent = TestUtils.indent(commonIndent);
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (String line : lines) {
			if (!line.isEmpty()) {
				if (first) {
					first = false;
				} else {
					sb.append(ICodeWriter.NL);
				}
				sb.append(indent);
				sb.append(line);
			}
		}
		return containsOnlyOnce(sb.toString());
	}

	public JadxCodeAssertions removeBlockComments() {
		String code = actual.replaceAll("/\\*.*\\*/", "");
		return new JadxCodeAssertions(code);
	}

	public JadxCodeAssertions print() {
		System.out.println("-----------------------------------------------------------");
		System.out.println(actual);
		System.out.println("-----------------------------------------------------------");
		return this;
	}
}
