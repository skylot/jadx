package jadx.tests.integration.enums;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestEnumAnonymous extends IntegrationTest {

	public static class TestCls {
		public enum EnumAnonymous1 {
			TEST {
			}
		}

		public enum EnumAnonymous2 {
			TEST1("TEST1", 0) {
			};

			EnumAnonymous2(final String string, final int i) {
			}
		}
	}

	@TestWithProfiles({ TestProfile.DX_J8, TestProfile.D8_J11, TestProfile.JAVA11 })
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("synthetic")
				.doesNotContain("Enum visitor error")
				.containsOne("public enum EnumAnonymous1 {")
				.containsOne("public enum EnumAnonymous2 {")
				.containsOne("TEST1(\"TEST1\", 0) {");
	}
}
