package jadx.tests.integration.switches;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestSwitchReturnFromCase extends IntegrationTest {

	public static class TestCls {
		public void test(int a) {
			if (a > 1000) {
				return;
			}
			String s = null;
			switch (a % 10) {
				case 1:
					s = "1";
					break;
				case 2:
					s = "2";
					break;
				case 3:
				case 4:
					s = "4";
					break;
				case 5:
					break;
				case 6:
					return;
			}
			if (s == null) {
				s = "5";
			}
			System.out.println(s);
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("switch (a % 10) {")
				// case 5: removed
				.countString(5, "case ")
				.countString(3, "break;")
				.containsOne("s = \"1\";")
				.containsOne("s = \"2\";")
				.containsOne("s = \"4\";")
				.containsOne("s = \"5\";");
	}
}
