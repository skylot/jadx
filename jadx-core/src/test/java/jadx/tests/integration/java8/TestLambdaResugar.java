package jadx.tests.integration.java8;

import java.util.function.Function;

import jadx.NotYetImplemented;
import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestLambdaResugar extends IntegrationTest {

	public static class TestCls {
		private String field;

		public void test() {
			call(s -> {
				this.field = s;
				return s.length();
			});
		}

		public void call(Function<String, Integer> func) {
		}
	}

	@NotYetImplemented("Inline lambda methods")
	@TestWithProfiles(TestProfile.D8_J11_DESUGAR)
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("lambda$");
	}
}
