package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

/**
 * Issue 1407
 */
public class TestTypeResolver19 extends SmaliTest {

	public static class TestCls {
		public static int[] test(byte[] bArr) {
			int[] iArr = new int[bArr.length];
			for (int i = 0; i < bArr.length; i++) {
				iArr[i] = bArr[i];
			}
			return iArr;
		}

		public static int[] test2(byte[] bArr) {
			int[] iArr = new int[bArr.length];
			for (int i = 0; i < bArr.length; i++) {
				int i2 = bArr[i];
				if (i2 < 0) {
					i2 = (int) ((long) i2 & 0xFFFF_FFFFL);
				}
				iArr[i] = i2;
			}
			return iArr;
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("iArr[i] = bArr[i];")
				.containsOne("iArr[i] = i2;");
	}
}
