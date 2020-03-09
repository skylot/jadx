package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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
			assertThat(runTest(""), is("bad, str: "));
			assertThat(runTest("1234"), is("good, len: 4, str: 1234"));
			assertThat(runTest("1234567"), is("bad, str: 1234567"));
		}
	}

	@Test
	public void test() {
		noDebugInfo();

		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("str.length()"));
		assertThat(code, containsOne("System.out.println(\"done\");"));
	}
}
