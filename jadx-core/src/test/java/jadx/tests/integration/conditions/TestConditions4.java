package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestConditions4 extends IntegrationTest {

	public static class TestCls {
		public int test(int num) {
			boolean inRange = (num >= 59 && num <= 66);
			return inRange ? num + 1 : num;
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("num >= 59 && num <= 66")
				.contains("? num + 1 : num;").doesNotContain("else");
	}
}
