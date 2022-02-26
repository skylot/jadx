package jadx.tests.integration.others;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.stream.Collectors;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestMethodParametersAttribute extends IntegrationTest {

	public static class TestCls {
		public String test(String paramStr, final int number) {
			return paramStr + number;
		}

		public String paramNames() throws NoSuchMethodException {
			Method testMethod = TestCls.class.getMethod("test", String.class, int.class);
			return Arrays.stream(testMethod.getParameters())
					.map(Parameter::getName)
					.collect(Collectors.joining(", "));
		}

		public void check() throws NoSuchMethodException {
			assertThat(paramNames()).isEqualTo("paramStr, number");
		}
	}

	@TestWithProfiles({ TestProfile.JAVA8, TestProfile.D8_J11 })
	public void test() {
		getCompilerOptions().addArgument("-parameters");
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("public String test(String paramStr, final int number) {");
	}
}
