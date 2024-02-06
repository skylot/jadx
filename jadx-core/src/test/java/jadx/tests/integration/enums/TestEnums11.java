package jadx.tests.integration.enums;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

@SuppressWarnings("unused")
public class TestEnums11 extends IntegrationTest {

	public static class TestCls {
		public enum Mode {
			FIRST,
			SECOND,
			THIRD;

			private static final Mode DEFAULT = THIRD;

			public static Mode getDefault() {
				return DEFAULT;
			}
		}
	}

	@TestWithProfiles(TestProfile.D8_J11)
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("private static final Mode DEFAULT = THIRD;");
	}
}
