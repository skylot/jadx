package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsLines;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

public class TestArrayForEach2 extends IntegrationTest {

	public static class TestCls {
		public void test(String str) {
			for (String s : str.split("\n")) {
				String t = s.trim();
				if (t.length() > 0) {
					System.out.println(t);
				}
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("int ")));

		assertThat(code, containsLines(2,
				"for (String s : str.split(\"\\n\")) {",
				indent(1) + "String t = s.trim();",
				indent(1) + "if (t.length() > 0) {",
				indent(2) + "System.out.println(t);",
				indent(1) + '}',
				"}"));
	}
}
