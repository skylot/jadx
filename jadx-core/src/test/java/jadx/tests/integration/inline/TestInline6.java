package jadx.tests.integration.inline;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestInline6 extends IntegrationTest {

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

		assertThat(code, containsString("System.out.println(System.nanoTime() - start);"));
		assertThat(code, not(containsString("System.out.println(System.nanoTime() - System.nanoTime());")));
	}
}
