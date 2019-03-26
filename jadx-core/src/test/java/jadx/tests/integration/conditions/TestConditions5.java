package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("if (a1 == null) {"));
		assertThat(code, containsString("if (a2 != null) {"));
		assertThat(code, containsString("throw new AssertionError(a1 + \" != \" + a2);"));
		assertThat(code, not(containsString("if (a1.equals(a2)) {")));
		assertThat(code, containsString("} else if (!a1.equals(a2)) {"));
	}
}
