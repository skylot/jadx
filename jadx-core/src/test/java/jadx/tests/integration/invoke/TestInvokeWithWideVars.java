package jadx.tests.integration.invoke;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestInvokeWithWideVars extends IntegrationTest {

	@SuppressWarnings("SameParameterValue")
	public static class TestCls {

		public long test1() {
			return call(1, 2L);
		}

		public long test2() {
			return rangeCall(1L, 2, 3.0d, (byte) 4);
		}

		private long call(int a, long b) {
			return 0L;
		}

		private long rangeCall(long a, int b, double c, byte d) {
			return 0L;
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("return call(1, 2L);")
				.containsOne("return rangeCall(1L, 2, 3.0d, (byte) 4);");
	}
}
