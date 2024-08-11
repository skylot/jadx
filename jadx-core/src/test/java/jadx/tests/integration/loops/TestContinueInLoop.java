package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestContinueInLoop extends IntegrationTest {

	public static class TestCls {
		public int f;

		public void test(int[] a, int b) {
			for (int i = 0; i < a.length; i++) {
				int v = a[i];
				if (v < b) {
					a[i]++;
				} else if (v > b) {
					a[i]--;
				} else {
					continue;
				}
				if (i < b) {
					break;
				}
			}
			this.f++;
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("for (int i = 0; i < a.length; i++) {")
				.containsOne("if (i < b) {")
				.containsOne("continue;")
				.containsOne("break;")
				.containsOne("this.f++;");
	}
}
