package jadx.tests.integration.arrays;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestArrayFillNegative extends IntegrationTest {

	public static class TestCls {
		public int[] test() {
			int[] arr = new int[3];
			arr[0] = 1;
			arr[1] = arr[0] + 1;
			arr[2] = arr[1] + 1;
			return arr;
		}

		public void check() {
			assertThat(test()).isEqualTo(new int[] { 1, 2, 3 });
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("int[] arr = {1, ")
				.containsOne("int[] arr = new int[3];")
				.containsOne("arr[1] = arr[0] + 1;");
	}
}
