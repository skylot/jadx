package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestWrongCode extends IntegrationTest {

	public static class TestCls {
		@SuppressWarnings("null")
		public int test() {
			int[] a = null;
			return a.length;
		}

		public int test2(int a) {
			if (a == 0) {
			}
			return a;
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("return false.length;")
				.containsOne("int[] a = null;")
				.containsOne("return a.length;")
				.containsLines(2,
						"if (a == 0) {",
						"}",
						"return a;");
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		getClassNode(TestCls.class);
	}
}
