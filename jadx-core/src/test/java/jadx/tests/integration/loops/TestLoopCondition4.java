package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestLoopCondition4 extends IntegrationTest {

	public static class TestCls {
		public static void test() {
			int n = -1;
			while (n < 0) {
				n += 12;
			}
			while (n > 11) {
				n -= 12;
			}
			System.out.println(n);
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("int n = -1;")
				.containsOne("while (n < 0) {")
				.containsOne("n += 12;")
				.containsOne("while (n > 11) {")
				.containsOne("n -= 12;")
				.containsOne("System.out.println(n);");
	}
}
