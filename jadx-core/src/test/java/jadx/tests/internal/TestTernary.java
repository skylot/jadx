package jadx.tests.internal;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.either;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TestTernary extends InternalJadxTest {

	public static class TestCls {
		public boolean test1(int a) {
			return a != 2;
		}

		public void test2(int a) {
			assertTrue(a == 3);
		}

		public int test3(int a) {
			return a > 0 ? 1 : (a + 2) * 3;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("else")));
		assertThat(code, containsString("return a != 2;"));
		assertThat(code, containsString("assertTrue(a == 3)"));
		assertThat(code, either(containsString("return a > 0 ? 1 : (a + 2) * 3;"))
				.or(containsString("return (a > 0) ? 1 : (a + 2) * 3;")));
	}
}
