package jadx.tests.integration.switches;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSwitchInLoop2 extends IntegrationTest {
	public static class TestCls {
		public boolean test() {
			while (true) {
				switch (call()) {
					case 0:
						return false;
					case 1:
						return true;
				}
			}
		}

		private int call() {
			return 0;
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("while (true) {")
				.containsOne("switch (call()) {");
	}
}
