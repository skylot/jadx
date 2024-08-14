package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestLoopDetection2 extends IntegrationTest {

	public static class TestCls {

		public int test(int a, int b) {
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
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("int c = a + b;")
				.containsOne("for (int i = a; i < b; i++) {")
				.doesNotContain("c_2");
	}
}
