package jadx.tests.integration.types;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTypeResolver4 extends IntegrationTest {

	public static class TestCls {

		private static String test(byte[] strArray, int offset) {
			int len = strArray.length;
			int start = offset + f(strArray, offset);
			int end = start;
			while (end + 1 < len && (strArray[end] != 0 || strArray[end + 1] != 0)) {
				end += 2;
			}
			byte[] arr = Arrays.copyOfRange(strArray, start, end);
			return new String(arr);
		}

		private static int f(byte[] strArray, int offset) {
			return 0;
		}

		public void check() {
			String test = test(("1234" + "utfstr\0\0" + "4567").getBytes(), 4);
			assertThat(test).isEqualTo("utfstr");
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("(strArray[end] != 0 || strArray[end + 1] != 0)");
	}

	@Test
	public void test2() {
		noDebugInfo();
		getClassNode(TestCls.class);
	}
}
