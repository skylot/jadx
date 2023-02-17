package jadx.tests.integration.java8;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestLambdaExtVar2 extends IntegrationTest {

	public static class TestCls {

		public void test(List<String> list) {
			String space = " ";
			list.removeIf(s -> s.equals(space) || s.contains(space));
		}

		public void check() {
			List<String> list = new ArrayList<>(Arrays.asList("a", " ", "b", "r "));
			test(list);
			assertThat(list).isEqualTo(Arrays.asList("a", "b"));
		}
	}

	@TestWithProfiles({ TestProfile.DX_J8, TestProfile.D8_J11, TestProfile.JAVA11 })
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("lambda$")
				.containsOne("String space = \" \";")
				.containsOne("s.equals(space) || s.contains(space)");
	}
}
