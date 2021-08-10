package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTypeResolver3 extends IntegrationTest {

	@SuppressWarnings("UseCompareMethod")
	public static class TestCls {

		public int test(String s1, String s2) {
			int cmp = s2.compareTo(s1);
			if (cmp != 0) {
				return cmp;
			}
			return s1.length() == s2.length() ? 0 : s1.length() < s2.length() ? -1 : 1;
		}
	}

	@Test
	public void test() {
		useJavaInput();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOneOf(
						"return s1.length() == s2.length() ? 0 : s1.length() < s2.length() ? -1 : 1;",
						"return s1.length() < s2.length() ? -1 : 1;");
	}

	@Test
	public void test2() {
		noDebugInfo();
		getClassNode(TestCls.class);
	}
}
