package jadx.tests.integration.others;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertThat;

public class TestLoopInTry extends IntegrationTest {

	public static class TestCls {
		private static boolean b = true;

		public int test() {
			try {
				if (b) {
					throw new Exception();
				}
				while (f()) {
					s();
				}
			} catch (Exception e) {
				System.out.println("exception");
				return 1;
			}
			return 0;
		}

		private static void s() {
		}

		private static boolean f() {
			return false;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("try {"));
		assertThat(code, containsOne("if (b) {"));
		assertThat(code, containsOne("throw new Exception();"));
		assertThat(code, containsOne("while (f()) {"));
		assertThat(code, containsOne("s();"));
		assertThat(code, containsOne("} catch (Exception e) {"));
		assertThat(code, containsOne("return 1;"));
		assertThat(code, containsOne("return 0;"));
	}
}
