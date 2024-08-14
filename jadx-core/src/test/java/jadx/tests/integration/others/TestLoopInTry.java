package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

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
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("try {")
				.containsOne("if (b) {")
				.containsOne("throw new Exception();")
				.containsOne("while (f()) {")
				.containsOne("s();")
				.containsOne("} catch (Exception e) {")
				.containsOne("return 1;")
				.containsOne("return 0;");
	}
}
