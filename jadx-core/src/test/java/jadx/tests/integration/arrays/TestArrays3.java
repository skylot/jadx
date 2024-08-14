package jadx.tests.integration.arrays;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestArrays3 extends IntegrationTest {
	public static class TestCls {

		private Object test(byte[] bArr) {
			return new Object[] { bArr };
		}

		public void check() {
			byte[] inputArr = { 1, 2 };
			Object result = test(inputArr);
			assertThat(result).isInstanceOf(Object[].class);
			assertThat(((Object[]) result)[0]).isEqualTo(inputArr);
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("return new Object[]{bArr};");
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("return new Object[]{bArr};");
	}
}
