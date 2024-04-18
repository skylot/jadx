package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestStringBuilderElimination5 extends IntegrationTest {

	public static class TestCls {
		@SuppressWarnings("StringConcatenationInLoop")
		public static String test(long[] a) {
			String s = "";
			final char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
			for (int i = a.length - 1; i >= 0; i--) {
				s += hexChars[(int) (a[i] >>> 60) & 0x0f];
				s += hexChars[(int) (a[i] >>> 56) & 0x0f];
				s += hexChars[(int) (a[i] >>> 52) & 0x0f];
				s += hexChars[(int) (a[i] >>> 48) & 0x0f];
				s += hexChars[(int) (a[i] >>> 44) & 0x0f];
				s += hexChars[(int) (a[i] >>> 40) & 0x0f];
				s += hexChars[(int) (a[i] >>> 36) & 0x0f];
				s += hexChars[(int) (a[i] >>> 32) & 0x0f];
				s += hexChars[(int) (a[i] >>> 28) & 0x0f];
				s += hexChars[(int) (a[i] >>> 24) & 0x0f];
				s += hexChars[(int) (a[i] >>> 20) & 0x0f];
				s += hexChars[(int) (a[i] >>> 16) & 0x0f];
				s += hexChars[(int) (a[i] >>> 12) & 0x0f];
				s += hexChars[(int) (a[i] >>> 8) & 0x0f];
				s += hexChars[(int) (a[i] >>> 4) & 0x0f];
				s += hexChars[(int) (a[i]) & 0x0f];
				s += " ";
			}
			return s;
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain(".append(");
	}
}
