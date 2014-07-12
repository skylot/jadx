package jadx.tests.internal.conditions;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

public class TestConditions2 extends InternalJadxTest {

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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
		System.out.println(code);

//		assertThat(code, containsString("return;"));
//		assertThat(code, not(containsString("else")));
	}
}
