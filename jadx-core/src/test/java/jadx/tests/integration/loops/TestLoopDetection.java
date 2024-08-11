package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestLoopDetection extends IntegrationTest {

	public static class TestCls {

		public void test(int[] a, int b) {
			int i = 0;
			while (i < a.length && i < b) {
				a[i]++;
				i++;
			}
			while (i < a.length) {
				a[i]--;
				i++;
			}
		}

		public void check() {
			int[] a = { 1, 1, 1, 1, 1 };
			test(a, 3);
			assertThat(a).containsExactly(new int[] { 2, 2, 2, 0, 0 });
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("while (i < a.length && i < b) {")
				.contains("while (i < a.length) {")
				.contains("int i = 0;")
				.doesNotContain("i_2")
				.contains("i++;");
	}
}
