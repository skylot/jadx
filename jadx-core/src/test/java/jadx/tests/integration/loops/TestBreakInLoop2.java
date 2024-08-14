package jadx.tests.integration.loops;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestBreakInLoop2 extends IntegrationTest {

	@SuppressWarnings({ "BusyWait", "ResultOfMethodCallIgnored" })
	public static class TestCls {
		public void test(List<Integer> data) throws Exception {
			for (;;) {
				try {
					funcB(data);
					break;
				} catch (Exception ex) {
					if (funcC()) {
						throw ex;
					}
					data.clear();
				}
				Thread.sleep(100L);
			}
		}

		private boolean funcB(List<Integer> data) {
			return false;
		}

		private boolean funcC() {
			return true;
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("while (true) {")
				.containsOneOf("break;", "return;")
				.containsOne("throw ex;")
				.containsOne("data.clear();")
				.containsOne("Thread.sleep(100L);");
	}
}
