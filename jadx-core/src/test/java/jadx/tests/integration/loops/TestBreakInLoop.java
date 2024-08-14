package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestBreakInLoop extends IntegrationTest {

	public static class TestCls {
		public int f;

		public void test(int[] a, int b) {
			for (int i = 0; i < a.length; i++) {
				a[i]++;
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
				.containsOne("break;")
				.containsOne("this.f++;")
				// .containsOne("a[i]++;")
				.countString(0, "else");
	}
}
