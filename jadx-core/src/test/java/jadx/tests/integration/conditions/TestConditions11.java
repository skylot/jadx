package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestConditions11 extends IntegrationTest {

	public static class TestCls {

		public void test(boolean a, int b) {
			if (a || b > 2) {
				f();
			}
		}

		private void f() {
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("if (a || b > 2) {")
				.containsOne("f();")
				.doesNotContain("return")
				.doesNotContain("else");
	}
}
