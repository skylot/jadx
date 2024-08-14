package jadx.tests.integration.inline;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestInline2 extends IntegrationTest {

	public static class TestCls {
		public int test() throws InterruptedException {
			int[] a = new int[] { 1, 2, 4, 6, 8 };
			int b = 0;
			for (int i = 0; i < a.length; i += 2) {
				b += a[i];
			}
			for (long i = b; i > 0; i--) {
				b += i;
			}
			return b;
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("int[] a = {1, 2, 4, 6, 8};")
				.containsOne("for (int i = 0; i < a.length; i += 2) {")
				.containsOne("for (long i2 = b; i2 > 0; i2--) {");
	}
}
