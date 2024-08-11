package jadx.tests.integration.switches;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSwitchBreak extends IntegrationTest {

	public static class TestCls {
		public String test(int a) {
			String s = "";
			loop: while (a > 0) {
				switch (a % 4) {
					case 1:
						s += "1";
						break;
					case 3:
					case 4:
						s += "4";
						break;
					case 5:
						s += "+";
						break loop;
				}
				s += "-";
				a--;
			}
			return s;
		}

		public void check() {
			assertThat(test(9)).isEqualTo("1--4--1--4--1-");
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.contains("switch (a % 4) {")
				.countString(4, "case ")
				.countString(2, "break;")
				.doesNotContain("default:")
				// TODO finish break with label from switch
				.containsOne("return s + \"+\";");
	}
}
