package jadx.tests.integration.conditions;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TestConditions16 extends IntegrationTest {

	public static class TestCls {
		private static boolean test(int a, int b) {
			return a < 0 || b % 2 != 0 && a > 28 || b < 0;
		}

		public void check() {
			assertTrue(test(-1, 1));
			assertTrue(test(1, -1));
			assertTrue(test(29, 3));
			assertFalse(test(2, 2));
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

//		assertThat(code, containsOne("return a < 0 || (b % 2 != 0 && a > 28) || b < 0;"));
		assertThat(code, containsOne("return a < 0 || ((b % 2 != 0 && a > 28) || b < 0);"));

	}
}
