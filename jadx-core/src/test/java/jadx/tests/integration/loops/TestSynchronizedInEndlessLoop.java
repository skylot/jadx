package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("synchronized (this) {"));
		assertThat(code, containsOne("try {"));
		assertThat(code, containsOne("f++;"));
		assertThat(code, containsOne("Thread.sleep(100L);"));
		assertThat(code, containsOne("} catch (Exception e) {"));
	}
}
