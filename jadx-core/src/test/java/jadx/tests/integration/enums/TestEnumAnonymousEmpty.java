package jadx.tests.integration.enums;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;
import jadx.tests.integration.enums.pkg.EnumAnonymousEmpty;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestEnumAnonymousEmpty extends IntegrationTest {

	@TestWithProfiles({ TestProfile.JAVA8, TestProfile.JAVA11, TestProfile.DX_J8, TestProfile.D8_J8, TestProfile.D8_J11 })
	public void test() {
		assertThat(getClassNode(EnumAnonymousEmpty.class))
				.code()
				.containsOne("enum EnumAnonymousEmpty")
				.containsOne("TEST {")
				.doesNotContain("synthetic", "AnonymousClass", "EnumAnonymousEmpty(");
	}
}
