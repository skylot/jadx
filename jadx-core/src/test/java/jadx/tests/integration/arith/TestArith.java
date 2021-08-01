package jadx.tests.integration.arith;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class TestArith extends IntegrationTest {

	public static class TestCls {

		public static final int F = 7;

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

		private static void use(int i) {
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
	}

	@Test
	@NotYetImplemented
	public void test2() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("a += 2;"));
		assertThat(code, containsString("a++;"));
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
	}

	@Test
	@NotYetImplemented
	public void testNoDebug2() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("i += 2;"));
		assertThat(code, containsString("i++;"));
	}
}
