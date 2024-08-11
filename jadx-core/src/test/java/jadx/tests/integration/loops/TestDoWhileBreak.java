package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestDoWhileBreak extends IntegrationTest {

	public static class TestCls {

		public int test(int k) throws InterruptedException {
			int i = 3;
			do {
				if (k > 9) {
					i = 0;
					break;
				}
				i++;
			} while (i < 5);

			return i;
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("while (");
	}
}
