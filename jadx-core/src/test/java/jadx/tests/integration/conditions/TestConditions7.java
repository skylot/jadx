package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestConditions7 extends IntegrationTest {

	public static class TestCls {
		public void test(int[] a, int i) {
			if (i >= 0 && i < a.length) {
				a[i]++;
			}
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("if (i >= 0 && i < a.length) {")
				.doesNotContain("||");
	}
}
