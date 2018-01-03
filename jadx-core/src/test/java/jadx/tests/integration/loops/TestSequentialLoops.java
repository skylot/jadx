package jadx.tests.integration.loops;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static jadx.tests.api.utils.JadxMatchers.countString;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestSequentialLoops extends IntegrationTest {

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
		disableCompilation();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, countString(2, "while ("));
		assertThat(code, containsOne("break;"));
		assertThat(code, containsOne("return c;"));
		assertThat(code, not(containsString("else")));
	}
}
