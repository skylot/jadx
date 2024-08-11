package jadx.tests.integration.switches;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

import static org.assertj.core.api.Assertions.assertThat;

public class TestSwitchInLoop extends IntegrationTest {
	public static class TestCls {
		public int test(int k) {
			int a = 0;
			while (true) {
				switch (k) {
					case 0:
						return a;
					default:
						a++;
						k >>= 1;
				}
			}
		}

		public void check() {
			assertThat(test(1)).isEqualTo(1);
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("switch (k) {")
				.containsOne("case 0:")
				.containsOne("return a;")
				.containsOne("default:")
				.containsOne("a++;")
				.containsOne("k >>= 1;");
	}
}
