package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

public class TestIndexForLoop extends IntegrationTest {

	public static class TestCls {

		private int test(int[] a, int b) {
			int sum = 0;
			for (int i = 0; i < b; i++) {
				sum += a[i];
			}
			return sum;
		}

		public void check() {
			int[] array = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
			assertThat(test(array, 0)).isEqualTo(0);
			assertThat(test(array, 3)).isEqualTo(6);
			assertThat(test(array, 8)).isEqualTo(36);
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsLines(2,
						"int sum = 0;",
						"for (int i = 0; i < b; i++) {",
						indent(1) + "sum += a[i];",
						"}",
						"return sum;");
	}
}
