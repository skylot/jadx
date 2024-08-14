package jadx.tests.integration.variables;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestVariables3 extends IntegrationTest {

	public static class TestCls {
		String test(Object s) {
			int i;
			if (s == null) {
				i = 2;
			} else {
				i = 3;
				s = null;
			}
			return s + " " + i;
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("int i;")
				.contains("i = 2;")
				.contains("i = 3;")
				.contains("s = null;")
				.contains("return s + \" \" + i;");
	}
}
