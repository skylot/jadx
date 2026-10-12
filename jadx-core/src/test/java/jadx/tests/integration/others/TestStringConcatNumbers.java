package jadx.tests.integration.others;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestStringConcatNumbers extends IntegrationTest {

	@SuppressWarnings({ "StringConcatenationMissingWhitespace", "unused" })
	public static class TestCls {
		private String str = "s";

		public String test1(int a, int b) {
			return "" + a + b;
		}

		public String test2(int a, int b, int c) {
			return "" + a + b + c;
		}

		public String test3(int a, int b) {
			return a + (b + "");
		}

		public void test4(char a, char b) {
			this.str = this.str + a + b;
		}

		public void check() {
			assertThat(test1(1, 2)).isEqualTo("12");
			assertThat(test2(3, 4, 5)).isEqualTo("345");
			assertThat(test3(6, 7)).isEqualTo("67");
			test4('a', 'b');
			assertThat(this.str).isEqualTo("sab");
		}
	}

	@TestWithProfiles({ TestProfile.DX_J8, TestProfile.JAVA11 })
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("return a + b")
				.doesNotContain("+= a + b;");
	}
}
