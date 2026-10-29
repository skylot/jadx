package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestDoWhileAfterSwitch extends IntegrationTest {

	public static class TestCls {
		private int calls;

		private int next(int v) {
			calls++;
			return v;
		}

		public String test(int n) {
			StringBuilder sb = new StringBuilder();
			switch (n) {
				case 1:
					sb.append('a');
					break;
				case 2:
					sb.append('b');
					break;
			}
			int k = 0;
			do {
				k += next(2);
			} while (k < 7 && next(1) > 0);
			sb.append(k);
			return sb.toString();
		}

		public void check() {
			assertThat(test(1)).isEqualTo("a8");
			assertThat(calls).isEqualTo(7);
			assertThat(test(3)).isEqualTo("8");
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsLines(2,
						"do {",
						indent(1) + "k += next(2);",
						indent(1) + "if (k >= 7) {",
						indent(2) + "break;",
						indent(1) + "}",
						"} while (next(1) > 0);",
						"sb.append(k);",
						"return sb.toString();")
				.countString(1, "return sb.toString();");
	}

	@Test
	public void testJavaInput() {
		useJavaInput();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsLines(2,
						"do {",
						indent(1) + "k += next(2);",
						indent(1) + "if (k >= 7) {",
						indent(2) + "break;",
						indent(1) + "}",
						"} while (next(1) > 0);",
						"sb.append(k);",
						"return sb.toString();")
				.countString(1, "return sb.toString();");
	}
}
