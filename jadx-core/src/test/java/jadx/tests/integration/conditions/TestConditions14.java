package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestConditions14 extends IntegrationTest {

	@SuppressWarnings({ "EqualsReplaceableByObjectsCall", "ConstantConditions" })
	public static class TestCls {

		public static boolean test(Object a, Object b) {
			boolean r = a == null ? b != null : !a.equals(b);
			if (r) {
				return false;
			}
			System.out.println("r=" + r);
			return true;
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsLines(2,
						"boolean r;",
						"if (a == null) {",
						indent() + "r = b != null;",
						"} else {",
						indent() + "r = !a.equals(b);",
						"}")
				.containsOne("if (r) {")
				.containsOne("System.out.println(\"r=\" + r);");
	}
}
