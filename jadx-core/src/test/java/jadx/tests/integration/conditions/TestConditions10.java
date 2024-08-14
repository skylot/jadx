package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestConditions10 extends IntegrationTest {

	public static class TestCls {

		public void test(boolean a, int b) {
			if (a || b > 2) {
				b++;
			}
			if (!a || (b >= 0 && b <= 11)) {
				System.out.println("1");
			} else {
				System.out.println("2");
			}
			System.out.println("3");
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("return")
				.containsOne("if (a || b > 2) {")
				.containsOne("b++;")
				.containsOne("if (!a || (b >= 0 && b <= 11)) {")
				.containsOne("System.out.println(\"1\");")
				.containsOne("} else {")
				.containsOne("System.out.println(\"2\");")
				.containsOne("System.out.println(\"3\");");
	}
}
