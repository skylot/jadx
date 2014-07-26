package jadx.tests.internal.loops;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static jadx.tests.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertThat;

public class TestLoopCondition3 extends InternalJadxTest {

	public static class TestCls {

		public static void test(int a, int b, int c) {
			while (a < 12) {
				if (b + a < 9 && b < 8) {
					if (b >= 2 && a > -1 && b < 6) {
						System.out.println("OK");
						c = b + 1;
					}
					b = a;
				}
				c = b;
				b++;
				b = c;
				a++;
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
		System.out.println(code);

		assertThat(code, containsOne("while (a < 12) {"));
		assertThat(code, containsOne("if (b + a < 9 && b < 8) {"));
		assertThat(code, containsOne("if (b >= 2 && a > -1 && b < 6) {"));
	}
}
