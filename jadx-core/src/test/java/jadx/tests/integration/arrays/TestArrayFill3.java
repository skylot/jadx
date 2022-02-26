package jadx.tests.integration.arrays;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestArrayFill3 extends IntegrationTest {

	public static class TestCls {
		public byte[] test() {
			return new byte[] { 0, 1, 2 };
		}
	}

	@TestWithProfiles({ TestProfile.ECJ_J8, TestProfile.ECJ_DX_J8 })
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("return new byte[]{0, 1, 2}");
	}
}
