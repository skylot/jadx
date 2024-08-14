package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestConditions17 extends IntegrationTest {

	public static class TestCls {

		public static final int SOMETHING = 2;

		public static void test(int a) {
			if ((a & SOMETHING) != 0) {
				print(1);
			}
			print(2);
		}

		public static void print(Object o) {
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne(" & ");
	}
}
