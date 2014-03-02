package jadx.tests.internal.inline;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class TestInline2 extends InternalJadxTest {

	public static class TestCls {
		public int simple_loops() throws InterruptedException {
			int[] a = new int[]{1, 2, 4, 6, 8};
			int b = 0;
			for (int i = 0; i < a.length; i++) {
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

		assertThat(code, containsString("i < a.length"));
	}
}
