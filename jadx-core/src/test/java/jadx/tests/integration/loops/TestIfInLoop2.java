package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestIfInLoop2 extends IntegrationTest {

	public static class TestCls {
		public static void test(String str) {
			int len = str.length();
			int at = 0;
			while (at < len) {
				char c = str.charAt(at);
				int endAt = at + 1;
				if (c == 'A') {
					while (endAt < len) {
						c = str.charAt(endAt);
						if (c == 'B') {
							break;
						}
						endAt++;
					}
				}
				at = endAt;
			}
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("for (int at = 0; at < len; at = endAt) {");
	}
}
