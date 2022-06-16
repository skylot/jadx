package jadx.tests.integration.types;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTypeResolver23 extends IntegrationTest {

	public static class TestCls {
		public long test(int a) {
			long v = 1L;
			if (a == 2) {
				v = 2L;
			} else if (a == 3) {
				v = 3L;
			}
			System.out.println(v);
			return v;
		}
	}

	@TestWithProfiles({ TestProfile.JAVA8, TestProfile.DX_J8 })
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("long v");
	}
}
