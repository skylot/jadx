package jadx.tests.integration.others;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestStringConcatWideArgs extends IntegrationTest {

	public static class TestCls {
		public String test(long l, String s, double d, int i) {
			return l + " " + s + " " + d + " " + i;
		}

		public void check() {
			assertThat(test(5L, "x", 2.5, 7)).isEqualTo("5 x 2.5 7");
		}
	}

	@TestWithProfiles(TestProfile.JAVA11)
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("return l + \" \" + s + \" \" + d + \" \" + i;");
	}
}
