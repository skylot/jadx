package jadx.tests.integration.java8;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestLambdaExtVar extends IntegrationTest {

	public static class TestCls {

		public void test(List<String> list, String str) {
			list.removeIf(s -> s.equals(str));
		}

		public void check() {
			List<String> list = new ArrayList<>(Arrays.asList("a", "str", "b"));
			test(list, "str");
			assertThat(list).isEqualTo(Arrays.asList("a", "b"));
		}
	}

	@TestWithProfiles(TestProfile.DX_J8)
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		assertThat(cls)
				.code()
				.doesNotContain("lambda$")
				.containsOne("return s.equals(str);"); // TODO: simplify to expression
	}
}
