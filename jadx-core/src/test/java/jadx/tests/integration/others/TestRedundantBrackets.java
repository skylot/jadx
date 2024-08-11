package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestRedundantBrackets extends IntegrationTest {

	public static class TestCls {
		public boolean method(String str) {
			return str.indexOf('a') != -1;
		}

		public int method2(Object obj) {
			if (obj instanceof String) {
				return ((String) obj).length();
			}
			return 0;
		}

		public int method3(int a, int b) {
			if (a + b < 10) {
				return a;
			}
			if ((a & b) != 0) {
				return a * b;
			}
			return b;
		}

		public void method4(int num) {
			if (num == 4 || num == 6 || num == 8 || num == 10) {
				method2(null);
			}
		}

		public void method5(int[] a, int n) {
			a[1] = n * 2;
			a[n - 1] = 1;
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("(-1)")
				.doesNotContain("return;")
				.contains("if (obj instanceof String) {")
				.contains("return ((String) obj).length();")
				.contains("a + b < 10")
				.contains("(a & b) != 0")
				.contains("if (num == 4 || num == 6 || num == 8 || num == 10)")
				.contains("a[1] = n * 2;")
				.contains("a[n - 1] = 1;")
				.contains("public int method2(Object obj) {")
				// argument type isn't changed to String
				// cast not eliminated
				.contains("((String) obj).length()");
	}
}
