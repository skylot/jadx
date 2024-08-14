package jadx.tests.integration.arith;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestFieldIncrement extends IntegrationTest {

	public static class TestCls {
		public int instanceField = 1;
		public static int staticField = 1;
		public static String result = "";

		public void method() {
			instanceField++;
		}

		public void method2() {
			staticField--;
		}

		public void method3(String s) {
			result += s + '_';
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("instanceField++;")
				.contains("staticField--;")
				.contains("result += s + '_';");
	}
}
