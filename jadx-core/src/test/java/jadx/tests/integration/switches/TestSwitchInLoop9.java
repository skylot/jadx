package jadx.tests.integration.switches;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSwitchInLoop9 extends IntegrationTest {

	public static class TestCls {
		private void test() {
			int i = 0;
			int n = getN();
			while (true) {
				if (i > n) {
					break;
				}
				i++;
				if (n == 5) {
					continue;
				}
				switch (n) {
					case 0: {
						if (n != 1) {
							return;
						}
						break;
					}
					case 1:
						i++;
						break;
					default:
						continue;
				}
				if (i < 2) {
					i += 327;
				}
			}
			return;
		}

		private int getN() {
			double i = Math.random();
			if (i < 0.25) {
				return 1;
			}
			if (i < 0.5) {
				return 2;
			}
			if (i < 0.75) {
				return 3;
			}
			if (i < 1.0) {
				return 4;
			}
			return -1;
		}
	}

	@Test
	public void test() {
		// Checks that the work after the switch is recovered only once
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("switch (n) {")
				.containsOne("case 0:")
				.containsOne("case 1:")
				.containsOne("while (")
				.containsOne("default")
				.containsOne("327");
	}
}
