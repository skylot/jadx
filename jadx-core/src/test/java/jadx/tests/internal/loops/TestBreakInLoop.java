package jadx.tests.internal.loops;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static jadx.tests.utils.JadxMatchers.containsOne;
import static jadx.tests.utils.JadxMatchers.countString;
import static org.junit.Assert.assertThat;

public class TestBreakInLoop extends InternalJadxTest {

	public static class TestCls {
		private int f;

		private void test(int[] a, int b) {
			for (int i = 0; i < a.length; i++) {
				a[i]++;
				if (i < b) {
					break;
				}
			}
			this.f++;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
		System.out.println(code);

		assertThat(code, containsOne("for (int i = 0; i < a.length; i++) {"));
//		assertThat(code, containsOne("a[i]++;"));
		assertThat(code, containsOne("if (i < b) {"));
		assertThat(code, containsOne("break;"));
		assertThat(code, containsOne("this.f++;"));

		assertThat(code, countString(0, "else"));
	}
}
