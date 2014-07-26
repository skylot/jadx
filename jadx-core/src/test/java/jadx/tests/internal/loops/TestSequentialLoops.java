package jadx.tests.internal.loops;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static jadx.tests.utils.JadxMatchers.containsOne;
import static jadx.tests.utils.JadxMatchers.countString;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestSequentialLoops extends InternalJadxTest {

	public static class TestCls {
		public int test7(int a, int b) {
			int c = b;
			int z;

			while (true) {
				z = c + a;
				if (z >= 7) {
					break;
				}
				c = z;
			}

			while ((z = c + a) >= 7) {
				c = z;
			}
			return c;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
		System.out.println(code);

		assertThat(code, countString(2, "while ("));
		assertThat(code, containsOne("break;"));
		assertThat(code, containsOne("return c;"));
		assertThat(code, not(containsString("else")));
	}
}
