package jadx.tests.integration.enums;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;
import jadx.tests.integration.enums.pkg.EnumAnonymousArgs;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestEnumAnonymousArgs extends IntegrationTest {

	@TestWithProfiles({ TestProfile.JAVA8, TestProfile.JAVA11, TestProfile.DX_J8, TestProfile.D8_J8, TestProfile.D8_J11 })
	public void test() {
		assertThat(getClassNode(EnumAnonymousArgs.class))
				.code()
				.containsOne("enum EnumAnonymousArgs")
				.containsOne("TEST1(\"TEST1\", 0) {")
				.doesNotContain("synthetic", "AnonymousClass");
	}
}
