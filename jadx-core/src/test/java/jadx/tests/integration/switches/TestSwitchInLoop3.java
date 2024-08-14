package jadx.tests.integration.switches;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSwitchInLoop3 extends IntegrationTest {

	@SuppressWarnings("SwitchStatementWithTooFewBranches")
	public static class TestCls {
		public int test(int k) {
			int a = 0;
			while (true) {
				int x = 0; // keep this: force to generate the necessary CFG
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
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsLines(3,
						"switch (k) {",
						indent() + "case 0:",
						indent(2) + "return a;",
						indent() + "default:",
						indent(2) + "a++;",
						indent(2) + "k >>= 1;");
	}
}
