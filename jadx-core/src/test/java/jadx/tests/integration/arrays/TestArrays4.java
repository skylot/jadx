package jadx.tests.integration.arrays;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestArrays4 extends IntegrationTest {

	public static class TestCls {
		char[] payload;

		public TestCls(byte[] bytes) {
			char[] a = toChars(bytes);
			this.payload = new char[a.length];
			System.arraycopy(a, 0, this.payload, 0, bytes.length);
		}

		private static char[] toChars(byte[] bArr) {
			return new char[bArr.length];
		}
	}

	@Test
	public void testArrayTypeInference() {
		noDebugInfo();
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("char[] chars = toChars(bArr);");
	}
}
