package jadx.tests.integration.switches;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSwitchInLoop5 extends IntegrationTest {

	public static class TestCls {
		private static int test(int r) {
			int i;
			while (true) {
				switch (r) {
					case 42:
						i = 32;
						break;
					case 52:
						i = 42;
						break;
					default:
						System.out.println("Default switch case");
						return 1;
				}
				r = i;
			}
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("default:")
				.containsOne("System.out.println(");
	}
}
