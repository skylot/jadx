package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestArgInline extends IntegrationTest {

	public static class TestCls {

		public void test(int a) {
			while (a < 10) {
				int b = a + 1;
				a = b;
			}
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("i++;")
				.doesNotContain("i = i + 1;");
	}
}
