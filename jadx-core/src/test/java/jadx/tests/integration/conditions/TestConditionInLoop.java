package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestConditionInLoop extends IntegrationTest {

	public static class TestCls {
		private static int test(int a, int b) {
			int c = a + b;
			for (int i = a; i < b; i++) {
				if (i == 7) {
					c += 2;
				} else {
					c *= 2;
				}
			}
			c--;
			return c;
		}

		public void check() {
			assertThat(test(5, 9)).isEqualTo(115);
			assertThat(test(8, 23)).isEqualTo(1015807);
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("for (int i = a; i < b; i++) {")
				.containsOne("c += 2;")
				.containsOne("c *= 2;");
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("while");
	}
}
