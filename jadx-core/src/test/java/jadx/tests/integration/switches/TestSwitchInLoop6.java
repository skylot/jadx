package jadx.tests.integration.switches;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSwitchInLoop6 extends IntegrationTest {

	public static class TestCls {
		private void test() throws Exception {
			while (true) {
				int n = getN();
				switch (n) {
					case 1:
						n1();
						return;
					case 2:
						n2();
						if (getN() == 3) {
							return;
						}
						break;
					case 3:
						n3();
						return;
					case 4:
						n4();
						return;
					default:
						throw new Exception();
				}
			}
		}
		// Output below:
		// @formatter:off
		/*
			public void function() throws Exception {
				do {
					switch (getN()) {
						case 1:
							n1();
							return;
						case 2:
							n2();
							break;
						case 3:
							n3();
							return;
						case 4:
							n4();
							return;
						default:
							throw new Exception();
					}
				} while (getN() != 3);
			}
		*/
		// @formatter:on

		void n1() {
		}

		void n2() {
		}

		void n3() {
		}

		void n4() {
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
		allowWarnInCode();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("switch (n) {")
				.containsOne("case 1:")
				.containsOne("case 2:")
				.containsOne("case 3:")
				.containsOne("case 4:")
				.containsOne("do {")
				.containsOne("while (getN() != 3)");
	}
}
