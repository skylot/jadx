package jadx.tests.internal.loops;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class TestLoopCondition2 extends InternalJadxTest {

	public static class TestCls {

		public int test(boolean a) {
			int i = 0;
			while (a && i < 10) {
				i++;
			}
			return i;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
		System.out.println(code);

		assertThat(code, containsString("while (a && i < 10) {"));
	}
}
