package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestTernaryOneBranchInConstructor extends IntegrationTest {

	public static class TestCls {
		public TestCls(String str, int i) {
			this(str == null ? 0 : i);
		}

		public TestCls(int i) {
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("this(str == null ? 0 : i);")
				.doesNotContain("//")
				.doesNotContain("call moved to the top of the method");
	}

	public static class TestCls2 {
		public TestCls2(String str, int i) {
			this(i == 1 ? str : "", i == 0 ? "" : str);
		}

		public TestCls2(String a, String b) {
		}
	}

	@Test
	public void test2() {
		noDebugInfo();
		JadxAssertions.assertThat(getClassNode(TestCls2.class))
				.code()
				.containsOne("this(i == 1 ? str : \"\", i == 0 ? \"\" : str);")
				.doesNotContain("//");
	}
}
