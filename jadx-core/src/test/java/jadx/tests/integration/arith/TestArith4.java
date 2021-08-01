package jadx.tests.integration.arith;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestArith4 extends IntegrationTest {

	public static class TestCls {
		public static byte test(byte b) {
			int k = b & 7;
			return (byte) (((b & 255) >>> (8 - k)) | (b << k));
		}

		public static int test2(String str) {
			int k = 'a' | str.charAt(0);
			return (1 - k) & (1 + k);
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("int k = b & 7;")
				.containsOne("& 255")
				.containsOneOf("return (1 - k) & (1 + k);", "return (1 - k) & (k + 1);");
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("& 255");
	}
}
