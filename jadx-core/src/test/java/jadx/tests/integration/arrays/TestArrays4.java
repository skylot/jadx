package jadx.tests.integration.arrays;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("char[] chars = toChars(bArr);"));
	}
}
