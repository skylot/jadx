package jadx.tests.integration.code;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestArrayAccessReorder extends IntegrationTest {

	public static class TestCls {
		public int[] test(int[] arr) {
			int len = arr.length;
			int[] result = new int[len];
			int i = 0;
			int k = len;
			while (k != 0) {
				int v = arr[i];
				k--;
				int t = -v;
				i++;
				result[k] = t * 5;
			}
			return result;
		}

		public void check() {
			assertThat(test(new int[] { 1, 2, 3 })).isEqualTo(new int[] { -15, -10, -5 });
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("i++");
	}
}
