package jadx.tests.internal.conditions;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TestTernary2 extends InternalJadxTest {

	public static class TestCls {

		public void test() {
			assertTrue(f(1, 0) == 0);
		}

		private int f(int a, int b) {
			return a + b;
		}
	}

	//@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
		System.out.println(code);

		assertThat(code, containsString("assertTrue(f(1, 0) == 0);"));
	}
}
