package jadx.tests.integration.arith;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

public class TestArith extends IntegrationTest {

	public static class TestCls {

		public int test(int a) {
			a += 2;
			use(a);
			return a;
		}

		public int test2(int a) {
			a++;
			use(a);
			return a;
		}

		private static void use(int i) {}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		// TODO: reduce code vars by name
//		assertThat(code, containsString("a += 2;"));
//		assertThat(code, containsString("a++;"));
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		// TODO: simplify for variables without debug names
//		assertThat(code, containsString("i += 2;"));
//		assertThat(code, containsString("i++;"));
	}
}
