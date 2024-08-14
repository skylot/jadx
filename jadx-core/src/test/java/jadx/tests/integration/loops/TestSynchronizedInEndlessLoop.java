package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestSynchronizedInEndlessLoop extends IntegrationTest {

	@SuppressWarnings("BusyWait")
	public static class TestCls {
		int f = 5;

		int test() {
			while (true) {
				synchronized (this) {
					if (f > 7) {
						return 7;
					}
					if (f < 3) {
						return 3;
					}
				}
				try {
					f++;
					Thread.sleep(100L);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("synchronized (this) {")
				.containsOne("try {")
				.containsOne("f++;")
				.containsOne("Thread.sleep(100L);")
				.containsOne("} catch (Exception e) {");
	}
}
