package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestInnerAssign extends IntegrationTest {

	public static class TestCls {
		private String result;

		@SuppressWarnings("checkstyle:InnerAssignment")
		public void test(String str) {
			int len;
			if (str.isEmpty() || (len = str.length()) > 5) {
				result += "bad";
			} else {
				result += "good, len: " + len;
			}
			result += ", str: " + str;
			System.out.println("done");
		}

		private String runTest(String str) {
			result = "";
			test(str);
			return result;
		}

		public void check() {
			assertThat(runTest("")).isEqualTo("bad, str: ");
			assertThat(runTest("1234")).isEqualTo("good, len: 4, str: 1234");
			assertThat(runTest("1234567")).isEqualTo("bad, str: 1234567");
		}
	}

	@Test
	public void test() {
		noDebugInfo();

		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("str.length()")
				.containsOne("System.out.println(\"done\");");
	}
}
