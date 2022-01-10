package jadx.tests.integration.variables;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestVariablesInInlinedAssign extends IntegrationTest {

	public static class TestCls {
		public final int test(final char[] s) {
			int i;
			for (i = 0; i < s.length; i++) {
				final char c = s[i];
				if (c != 'a' && c != 'b') {
					break;
				}
			}
			return i;
		}
	}

	@TestWithProfiles({ TestProfile.DX_J8, TestProfile.JAVA8 })
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("char c");
	}
}
