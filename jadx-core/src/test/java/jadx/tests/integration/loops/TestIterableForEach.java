package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestIterableForEach extends IntegrationTest {

	public static class TestCls {
		public String test(Iterable<String> a) {
			StringBuilder sb = new StringBuilder();
			for (String s : a) {
				sb.append(s);
			}
			return sb.toString();
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsLines(2,
						"StringBuilder sb = new StringBuilder();",
						"for (String s : a) {",
						indent(1) + "sb.append(s);",
						"}",
						"return sb.toString();");
	}
}
