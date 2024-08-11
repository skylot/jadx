package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestComplexWhileLoop extends IntegrationTest {

	public static class TestCls {
		public static String test(String[] arr) {
			int index = 0;
			int length = arr.length;
			String str;
			while ((str = arr[index]) != null) {
				if (str.length() == 1) {
					return str.trim();
				}
				if (++index >= length) {
					index = 0;
				}
			}
			System.out.println("loop end");
			return "";
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("for (int at = 0; at < len; at = endAt) {");
	}
}
