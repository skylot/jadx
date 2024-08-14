package jadx.tests.integration.arrays;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestArrays extends IntegrationTest {
	public static class TestCls {

		public int test1(int i) {
			int[] a = new int[] { 1, 2, 3, 5 };
			return a[i];
		}

		public int test2(int i) {
			int[][] a = new int[i][i + 1];
			return a.length;
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("return new int[]{1, 2, 3, 5}[i];");
	}
}
