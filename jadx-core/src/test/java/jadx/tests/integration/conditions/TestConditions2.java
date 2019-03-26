package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

public class TestConditions2 extends IntegrationTest {

	public static class TestCls {
		int c;
		String d;
		String f;

		public void testComplexIf(String a, int b) {
			if (d == null || (c == 0 && b != -1 && d.length() == 0)) {
				c = a.codePointAt(c);
			} else {
				if (a.hashCode() != 0xCDE) {
					c = f.compareTo(a);
				}
			}
		}
	}

	@Test
	public void test() {
		getClassNode(TestCls.class);
	}
}
