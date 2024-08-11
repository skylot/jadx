package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestTypeResolver9 extends IntegrationTest {

	public static class TestCls {
		public int test(byte b) {
			return 16777216 * b;
		}

		public int test2(byte[] array, int offset) {
			return (array[offset] * 128) + (array[offset + 1] & 0xFF);
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("return 16777216 * b;")
				.doesNotContain("Byte.MIN_VALUE");
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		getClassNode(TestCls.class);
	}
}
