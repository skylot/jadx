package jadx.tests.integration.enums;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestEnumsWithTernary extends IntegrationTest {

	public enum TestCls {
		FIRST(useNumber() ? "1" : "A"),
		SECOND(useNumber() ? "2" : "B"),
		ANY(useNumber() ? "1" : "2");

		private final String str;

		TestCls(String str) {
			this.str = str;
		}

		public String getStr() {
			return str;
		}

		public static boolean useNumber() {
			return false;
		}
	}

	@TestWithProfiles({ TestProfile.DX_J8, TestProfile.D8_J8 })
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("ANY(useNumber() ? \"1\" : \"2\");")
				.doesNotContain("static {");
	}
}
