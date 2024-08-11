package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

import static org.assertj.core.api.Assertions.assertThat;

public class TestConditions16 extends IntegrationTest {

	public static class TestCls {
		private static boolean test(int a, int b) {
			return a < 0 || b % 2 != 0 && a > 28 || b < 0;
		}

		public void check() {
			assertThat(test(-1, 1)).isTrue();
			assertThat(test(1, -1)).isTrue();
			assertThat(test(29, 3)).isTrue();
			assertThat(test(2, 2)).isFalse();
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("return a < 0 || (b % 2 != 0 && a > 28) || b < 0;");
	}
}
