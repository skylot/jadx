package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestLoopCondition3 extends IntegrationTest {

	public static class TestCls {

		public static void test(int a, int b, int c) {
			while (a < 12) {
				if (b + a < 9 && b < 8) {
					if (b >= 2 && a > -1 && b < 6) {
						System.out.println("OK");
						c = b + 1;
					}
					b = a;
				}
				c = b;
				b++;
				b = c;
				a++;
			}
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("while (a < 12) {")
				.containsOne("if (b + a < 9 && b < 8) {")
				.containsOne("if (b >= 2 && a > -1 && b < 6) {");
	}
}
