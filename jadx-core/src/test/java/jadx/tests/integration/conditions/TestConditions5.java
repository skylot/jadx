package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestConditions5 extends IntegrationTest {

	public static class TestCls {
		public static void test(Object a1, Object a2) {
			if (a1 == null) {
				if (a2 != null) {
					throw new AssertionError(a1 + " != " + a2);
				}
			} else if (!a1.equals(a2)) {
				throw new AssertionError(a1 + " != " + a2);
			}
		}

		public static void test2(Object a1, Object a2) {
			if (a1 != null) {
				if (!a1.equals(a2)) {
					throw new AssertionError(a1 + " != " + a2);
				}
			} else {
				if (a2 != null) {
					throw new AssertionError(a1 + " != " + a2);
				}
			}
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("if (a1 == null) {")
				.contains("if (a2 != null) {")
				.contains("throw new AssertionError(a1 + \" != \" + a2);")
				.doesNotContain("if (a1.equals(a2)) {")
				.contains("} else if (!a1.equals(a2)) {");
	}
}
