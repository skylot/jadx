package jadx.tests.integration.trycatch;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestNestedTryCatch2 extends IntegrationTest {

	public static class TestCls {
		public void test() {
			try {
				try {
					call();
					call();
				} catch (Exception e) {
					exc(e);
				}
			} catch (Exception e) {
				exc(e);
			}
		}

		private void call() {
		}

		private void exc(Exception e) {
		}
	}

	@TestWithProfiles({ TestProfile.JAVA8, TestProfile.DX_J8 })
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.countString(2, "} catch (Exception ");
	}
}
