package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.RaungTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestStringConcatJava11 extends RaungTest {

	public static class TestCls {
		public String test(final String s) {
			return s + "test";
		}

		//@formatter:off
		/* Dynamic call looks like this:
		public String test(final String s) {
			return java.lang.invoke.StringConcatFactory.makeConcatWithConstants(
					java.lang.invoke.MethodHandles.lookup(),
					"makeConcatWithConstants",
					java.lang.invoke.MethodType.fromMethodDescriptorString("(Ljava/lang/String;)Ljava/lang/String;", this.getClass().getClassLoader()),
					"\u0001test"
			).dynamicInvoker().invoke(s);
		}
		*/
		//@formatter:on

		public String test2(final String s) {
			return s + "test" + s + 7;
		}
	}

	@Test
	public void test() {
		assertThat(getClassNodeFromRaung())
				.code()
				.containsOne("return str + \"test\";")
				.containsOne("return str + \"test\" + str + 7;");
	}

	@Test
	public void testJava8() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("return str + \"test\";")
				.containsOneOf(
						"return str + \"test\" + str + 7;",
						"return str + \"test\" + str + \"7\";"); // dynamic concat add const to string recipe
	}

	@TestWithProfiles({ TestProfile.D8_J11, TestProfile.JAVA11 })
	public void testJava11() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("return str + \"test\";")
				.containsOne("return str + \"test\" + str + \"7\";");
	}
}
