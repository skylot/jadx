package jadx.tests.integration.types;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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
			assertThat(test, is("utfstr"));
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("(strArray[end] != 0 || strArray[end + 1] != 0)"));
	}

	@Test
	public void test2() {
		noDebugInfo();
		getClassNode(TestCls.class);
	}
}
