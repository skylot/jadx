package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

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
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("int ")
				.containsLines(2,
						"for (String s : str.split(\"\\n\")) {",
						indent(1) + "String t = s.trim();",
						indent(1) + "if (t.length() > 0) {",
						indent(2) + "System.out.println(t);",
						indent(1) + '}',
						"}");
	}
}
