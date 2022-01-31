package jadx.tests.api.utils.assertj;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

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
		for (String line : lines) {
			sb.append(ICodeWriter.NL);
			if (line.isEmpty()) {
				// don't add common indent to empty lines
				continue;
			}
			String searchLine = indent + line;
			sb.append(searchLine);
			// check every line for easier debugging
			contains(searchLine);
		}
		return containsOnlyOnce(sb.substring(ICodeWriter.NL.length()));
	}

	public JadxCodeAssertions removeBlockComments() {
		String code = actual.replaceAll("/\\*.*\\*/", "");
		JadxCodeAssertions newCode = new JadxCodeAssertions(code);
		newCode.print();
		return newCode;
	}

	public JadxCodeAssertions removeLineComments() {
		String code = actual.replaceAll("//.*(?!$)", "");
		JadxCodeAssertions newCode = new JadxCodeAssertions(code);
		newCode.print();
		return newCode;
	}

	public JadxCodeAssertions print() {
		System.out.println("-----------------------------------------------------------");
		System.out.println(actual);
		System.out.println("-----------------------------------------------------------");
		return this;
	}

	public JadxCodeAssertions containsOneOf(String... substringArr) {
		int matches = 0;
		for (String substring : substringArr) {
			matches += TestUtils.count(actual, substring);
		}
		if (matches != 1) {
			failWithMessage("Expected only one match from <%s> but was <%d>", Arrays.toString(substringArr), matches);
		}
		return this;
	}

	@SuppressWarnings("UnusedReturnValue")
	@SafeVarargs
	public final JadxCodeAssertions oneOf(Function<JadxCodeAssertions, JadxCodeAssertions>... checks) {
		int passed = 0;
		List<Throwable> failed = new ArrayList<>();
		for (Function<JadxCodeAssertions, JadxCodeAssertions> check : checks) {
			try {
				check.apply(this);
				passed++;
			} catch (Throwable e) {
				failed.add(e);
			}
		}
		if (passed != 1) {
			failWithMessage("Expected only one match but passed: <%d>, failed: <%d>, details:\n<%s>",
					passed, failed.size(),
					failed.stream().map(Throwable::getMessage).collect(Collectors.joining("\nFailed check:\n ")));
		}
		return this;
	}
}
