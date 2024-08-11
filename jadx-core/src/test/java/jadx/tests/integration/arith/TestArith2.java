package jadx.tests.integration.arith;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestArith2 extends IntegrationTest {

	public static class TestCls {

		public int test1(int a) {
			return (a + 2) * 3;
		}

		public int test2(int a, int b, int c) {
			return a + b + c;
		}

		public boolean test3(boolean a, boolean b, boolean c) {
			return a | b | c;
		}

		public boolean test4(boolean a, boolean b, boolean c) {
			return a & b & c;
		}

		public int substract(int a, int b, int c) {
			return a - (b - c);
		}

		public int divide(int a, int b, int c) {
			return a / (b / c);
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("return (a + 2) * 3;")
				.doesNotContain("a + 2 * 3")
				.contains("return a + b + c;")
				.doesNotContain("return (a + b) + c;")
				.contains("return a | b | c;")
				.doesNotContain("return (a | b) | c;")
				.contains("return a & b & c;")
				.doesNotContain("return (a & b) & c;")
				.contains("return a - (b - c);")
				.doesNotContain("return a - b - c;")
				.contains("return a / (b / c);")
				.doesNotContain("return a / b / c;");
	}
}
