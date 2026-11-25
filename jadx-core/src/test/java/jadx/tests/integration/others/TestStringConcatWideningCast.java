package jadx.tests.integration.others;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestStringConcatWideningCast extends IntegrationTest {

	@SuppressWarnings("unused")
	public static class TestCls {
		public String test1(float f) {
			return "f=" + (double) f;
		}

		public String test2(char c) {
			return "c=" + (long) c;
		}

		public String test3(int i) {
			return "i=" + (float) i;
		}

		public String test4(int i) {
			return "i=" + (long) i;
		}

		public void check() {
			assertThat(test1(0.1f)).isEqualTo("f=0.10000000149011612");
			assertThat(test2('A')).isEqualTo("c=65");
			assertThat(test3(5)).isEqualTo("i=5.0");
			assertThat(test4(5)).isEqualTo("i=5");
		}
	}

	@TestWithProfiles({ TestProfile.D8_J11, TestProfile.JAVA11 })
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("(double) f")
				.containsOne("(long) c")
				.containsOne("(float) i")
				.containsOne("return \"i=\" + i;");
	}
}
