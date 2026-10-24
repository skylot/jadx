package jadx.tests.integration.invoke;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestCastInOverloadedInvoke5 extends IntegrationTest {

	@SuppressWarnings("unused")
	public static class TestCls {
		public String test1(int i) {
			return call((long) i);
		}

		public String test2(float f) {
			return call((double) f);
		}

		public String test3(char c) {
			return call((int) c);
		}

		public long test4(int i) {
			return Math.abs((long) i);
		}

		private static String call(char c) {
			return "char";
		}

		private static String call(int i) {
			return "int";
		}

		private static String call(long l) {
			return "long";
		}

		private static String call(float f) {
			return "float";
		}

		private static String call(double d) {
			return "double";
		}

		public void check() {
			assertThat(test1(1)).isEqualTo("long");
			assertThat(test2(1.5f)).isEqualTo("double");
			assertThat(test3('a')).isEqualTo("int");
			assertThat(test4(Integer.MIN_VALUE)).isEqualTo(2147483648L);
		}
	}

	@TestWithProfiles({ TestProfile.DX_J8, TestProfile.D8_J11, TestProfile.JAVA11 })
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("call((long) i)")
				.containsOne("call((double) f)")
				.containsOne("call((int) c)")
				.containsOne("Math.abs((long) i)");
	}
}
