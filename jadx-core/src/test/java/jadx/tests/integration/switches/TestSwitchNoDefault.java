package jadx.tests.integration.switches;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSwitchNoDefault extends IntegrationTest {

	public static class TestCls {
		public void test(int a) {
			String s = null;
			switch (a) {
				case 1:
					s = "1";
					break;
				case 2:
					s = "2";
					break;
				case 3:
					s = "3";
					break;
				case 4:
					s = "4";
					break;
			}
			System.out.println(s);
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.countString(4, "break;")
				.containsOne("System.out.println(s);");
	}
}
