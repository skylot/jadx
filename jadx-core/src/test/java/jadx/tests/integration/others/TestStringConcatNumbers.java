package jadx.tests.integration.others;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestStringConcatNumbers extends IntegrationTest {

	@SuppressWarnings({ "StringConcatenationMissingWhitespace", "unused" })
	public static class TestCls {
		private String str = "s";

		public String test(int a, int b) {
			return "" + a + b;
		}

		public String test2(int a, int b) {
			return a + (b + "");
		}

		public void test3(char a, char b) {
			this.str = this.str + a + b;
		}

		public void check() {
			assertThat(test(1, 2)).isEqualTo("12");
			assertThat(test2(1, 2)).isEqualTo("12");
			test3('a', 'b');
			assertThat(this.str).isEqualTo("sab");
		}
	}

	@TestWithProfiles({ TestProfile.DX_J8, TestProfile.D8_J8, TestProfile.D8_J11, TestProfile.JAVA11 })
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("return a + b")
				.doesNotContain("+= a + b;");
	}
}
