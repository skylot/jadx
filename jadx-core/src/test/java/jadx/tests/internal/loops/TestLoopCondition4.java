package jadx.tests.internal.loops;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static jadx.tests.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertThat;

public class TestLoopCondition4 extends InternalJadxTest {

	public static class TestCls {
		public static void test() {
			int n = -1;
			while (n < 0) {
				n += 12;
			}
			while (n > 11) {
				n -= 12;
			}
			System.out.println(n);
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
		System.out.println(code);

		assertThat(code, containsOne("int n = -1;"));
		assertThat(code, containsOne("while (n < 0) {"));
		assertThat(code, containsOne("n += 12;"));
		assertThat(code, containsOne("while (n > 11) {"));
		assertThat(code, containsOne("n -= 12;"));
		assertThat(code, containsOne("System.out.println(n);"));
	}
}
