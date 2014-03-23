package jadx.tests.internal.conditions;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestConditions5 extends InternalJadxTest {

	public static class TestCls {
		public static void assertEquals(Object a1, Object a2) {
			if (a1 == null) {
				if (a2 != null)
					throw new AssertionError(a1 + " != " + a2);
			} else if (!a1.equals(a2)) {
				throw new AssertionError(a1 + " != " + a2);
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
		System.out.println(code);

		assertThat(code, containsString("if (a1 == null) {"));
		assertThat(code, containsString("if (a2 != null) {"));
		assertThat(code, containsString("throw new AssertionError(a1 + \" != \" + a2);"));
		assertThat(code, not(containsString("if (a1.equals(a2)) {")));
		assertThat(code, containsString("} else if (!a1.equals(a2)) {"));
	}
}
