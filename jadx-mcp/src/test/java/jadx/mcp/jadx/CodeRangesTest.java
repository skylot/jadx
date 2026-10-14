package jadx.mcp.jadx;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CodeRangesTest {

	private static int[] rangeOf(String code, String declMarker) {
		CodeText text = new CodeText(code);
		int[] lines = CodeRanges.nodeLines(text, code.indexOf(declMarker), true);
		assertThat(lines).isNotNull();
		return lines;
	}

	private static String linesText(String code, int[] range) {
		CodeText text = new CodeText(code);
		return text.getText().substring(text.lineStart(range[0]), text.lineEnd(range[1]));
	}

	@Test
	void jadxWarnBlockWithStackTrace() {
		String code = String.join("\n",
				"public class A {", // 1
				"    void prev() {", // 2
				"    }", // 3
				"", // 4
				"    /* JADX WARN: Type inference fix 'apply assigned field type' failed", // 5
				"    java.lang.UnsupportedOperationException: ArgType.getObject(), call class: x", // 6
				"        at jadx.core.dex.instructions.args.ArgType.getObject(ArgType.java:600)", // 7
				"        at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.run(T.java:1)", // 8
				"     */", // 9
				"    public final void T(int a) {", // 10
				"        a++;", // 11
				"    }", // 12
				"}");
		int[] range = rangeOf(code, "public final void T");
		assertThat(range).containsExactly(5, 12);
		String text = linesText(code, range);
		assertThat(text.trim()).startsWith("/* JADX WARN").endsWith("}");
	}

	@Test
	void javadocAnnotationsAndLineComments() {
		String code = String.join("\n",
				"class A {", // 1
				"    int x;", // 2
				"", // 3
				"    /**", // 4
				"     * Docs", // 5
				"     */", // 6
				"    // line comment", // 7
				"    @Override", // 8
				"    /* inline */", // 9
				"    public String toString() {", // 10
				"        return \"}\";", // 11
				"    }", // 12
				"}");
		assertThat(rangeOf(code, "public String toString")).containsExactly(4, 12);
	}

	@Test
	void noPrefix() {
		String code = String.join("\n",
				"class A {",
				"    int x;",
				"    void run() {",
				"    }",
				"}");
		assertThat(rangeOf(code, "void run")).containsExactly(3, 4);
	}

	@Test
	void blockCommentAfterCodeNotIncluded() {
		// comment starts after code on the same line: belongs to previous statement
		String code = String.join("\n",
				"class A {", // 1
				"    int x = 1; /* trailing", // 2
				"       comment */", // 3
				"    void run() {", // 4
				"    }", // 5
				"}");
		assertThat(rangeOf(code, "void run")).containsExactly(4, 5);
	}

	@Test
	void unclosedCommentEndWithoutStartNotIncluded() {
		String code = String.join("\n",
				"   stray text */", // 1
				"    void run() {", // 2
				"    }");
		assertThat(rangeOf(code, "void run")).containsExactly(2, 3);
	}
}
