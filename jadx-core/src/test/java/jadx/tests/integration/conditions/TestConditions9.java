package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestConditions9 extends IntegrationTest {

	public static class TestCls {
		public void test(boolean a, int b) throws Exception {
			if (!a || (b >= 0 && b <= 11)) {
				System.out.println('1');
			} else {
				System.out.println('2');
			}
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("if (!a || (b >= 0 && b <= 11)) {")
				.containsOne("System.out.println('1');")
				.containsOne("} else {")
				.containsOne("System.out.println('2');")
				.doesNotContain("return;");
	}
}
