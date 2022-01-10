package jadx.tests.integration.invoke;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSuperInvoke2 extends IntegrationTest {

	public static class TestCls {
		@Override
		public String toString() {
			return super.toString();
		}

		public void check() {
			assertThat(new TestCls().toString()).containsOne("@");
		}
	}

	@TestWithProfiles({ TestProfile.DX_J8, TestProfile.JAVA8 })
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("return super.toString();");
	}
}
