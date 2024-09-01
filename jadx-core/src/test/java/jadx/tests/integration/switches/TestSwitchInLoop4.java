package jadx.tests.integration.switches;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSwitchInLoop4 extends IntegrationTest {

	@SuppressWarnings("SwitchStatementWithTooFewBranches")
	public static class TestCls {
		private static boolean test(String s, int start) {
			boolean foundSeparator = false;
			for (int i = start; i < s.length(); i++) {
				char c = s.charAt(i);
				switch (c) {
					case '.':
						foundSeparator = true;
						break;
				}
				if (foundSeparator) {
					break;
				}
			}
			return foundSeparator;
		}

		public void check() {
			assertThat(test("a.b", 0)).isTrue();
			assertThat(test("abc", 1)).isFalse();
		}
	}

	@TestWithProfiles({ TestProfile.DX_J8, TestProfile.D8_J11, TestProfile.JAVA11 })
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("switch (c) {")
				.containsOne("break;"); // allow replacing second 'break' with 'return'
	}
}
