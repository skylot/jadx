package jadx.tests.integration.switches;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSwitchDefaultOnly extends IntegrationTest {

	public static class TestCls {
		public Object doSomething(int i) {
			switch (i) {
				default:
					return null;
			}
		}

		public int testFallthrough(int i) {
			int r = 0;
			switch (i) {
				default:
					r = 1;
					break;
			}
			return r;
		}

		public Object testEmptyDefault(int i) {
			switch (i) {
				default:
			}
			return null;
		}
	}

	@Test
	public void test() {
		useTargetJavaVersion(8);
		useJavaInput();
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("Failed to find 'out' block for switch");
	}
}
