package jadx.tests.integration.inline;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertThat;

public class TestInline2 extends IntegrationTest {

	public static class TestCls {
		public int test() throws InterruptedException {
			int[] a = new int[]{1, 2, 4, 6, 8};
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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("int[] a = new int[]{1, 2, 4, 6, 8};"));
		assertThat(code, containsOne("for (int i = 0; i < a.length; i += 2) {"));
		assertThat(code, containsOne("for (long i2 = (long) b; i2 > 0; i2--) {"));
	}
}
