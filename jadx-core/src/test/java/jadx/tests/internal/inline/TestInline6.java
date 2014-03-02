package jadx.tests.internal.inline;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestInline6 extends InternalJadxTest {

	public static class TestCls {
		public void f() {
		}

		public void test(int a, int b) {
			long start = System.nanoTime();
			f();
			System.out.println(System.nanoTime() - start);
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
		System.out.println(code);

		assertThat(code, containsString("System.out.println(System.nanoTime() - start);"));
		assertThat(code, not(containsString("System.out.println(System.nanoTime() - System.nanoTime());")));
	}
}
