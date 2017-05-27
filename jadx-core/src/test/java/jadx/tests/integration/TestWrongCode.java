package jadx.tests.integration;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestWrongCode extends IntegrationTest {

	public static class TestCls {
		private int test() {
			int[] a = null;
			return a.length;
		}

		@SuppressWarnings("empty")
		private int test2(int a) {
			if (a == 0) {
				;
			}
			return a;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("return false.length;")));
		assertThat(code, containsOne("int[] a = null;"));
		assertThat(code, containsOne("return a.length;"));

		assertThat(code, containsString("return a == 0 ? a : a;"));
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		getClassNode(TestCls.class);
	}
}
