package jadx.tests.integration.trycatch;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTryCatch9 extends IntegrationTest {

	public static class TestCls {
		public Integer test(final Integer i) {
			if (i == null) {
				return null;
			}
			Integer res = null;
			try {
				if (i == 5) {
					res = 4;
				} else {
					res = 9;
				}
			} catch (final Exception ex) {
				logError(ex);
			}
			return res;
		}

		private void logError(Exception ex) {
		}

		public void check() {
			assertThat(test(5)).isEqualTo(4);
		}
	}

	@TestWithProfiles({ TestProfile.DX_J8, TestProfile.JAVA8 })
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("logError(ex);")
				.containsOne("Integer res = null;");
	}
}
