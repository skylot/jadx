package jadx.tests.internal.arith;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestArith2 extends InternalJadxTest {

	public static class TestCls {

		public int test1(int a) {
			return (a + 2) * 3;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
		System.out.println(code);

		assertThat(code, containsString("return (a + 2) * 3;"));
		assertThat(code, not(containsString("a + 2 * 3")));
	}
}
