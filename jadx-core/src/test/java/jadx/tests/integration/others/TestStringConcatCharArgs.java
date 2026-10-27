package jadx.tests.integration.others;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestStringConcatCharArgs extends IntegrationTest {

	@SuppressWarnings("StringConcatenationMissingWhitespace")
	public static class TestCls {
		public String test(char c) {
			return c + "";
		}

		public String testInt(int i) {
			return i + "";
		}

		public void check() {
			assertThat(test('q')).isEqualTo("q");
			assertThat(testInt(5)).isEqualTo("5");
		}
	}

	@TestWithProfiles(TestProfile.JAVA11)
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("return \"\" + c;")
				.containsOne("return \"\" + i;");
	}
}
