package jadx.tests.internal.loops;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestLoopDetection2 extends InternalJadxTest {

	public static class TestCls {

		public int test(int a, int b) {
			int c = a + b;
			for (int i = a; i < b; i++) {
				if (i == 7) {
					c += 2;
				} else {
					c *= 2;
				}
			}
			c--;
			return c;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
		System.out.println(code);

		assertThat(code, containsString("while (i < b) {"));
		assertThat(code, containsString("int c = a + b;"));
		assertThat(code, not(containsString("c_2")));
		assertThat(code, containsString("i++"));
	}
}
