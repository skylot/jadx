package jadx.tests.integration.switches;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestSwitchContinue extends IntegrationTest {

	public static class TestCls {
		public String test(int a) {
			String s = "";
			while (a > 0) {
				switch (a % 4) {
					case 1:
						s += "1";
						break;
					case 3:
					case 4:
						s += "4";
						break;
					case 5:
						a -= 2;
						continue;
				}
				s += "-";
				a--;
			}
			return s;
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("switch (a % 4) {")
				.countString(4, "case ")
				.countString(2, "break;")
				.containsOne("a -= 2;")
				.containsOne("continue;");
	}
}
