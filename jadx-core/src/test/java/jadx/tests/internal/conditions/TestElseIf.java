package jadx.tests.internal.conditions;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestElseIf extends InternalJadxTest {

	public static class TestCls {
		public int testIfElse(String str) {
			int r;
			if (str.equals("a")) {
				r = 1;
			} else if (str.equals("b")) {
				r = 2;
			} else if (str.equals("3")) {
				r = 3;
			} else if (str.equals("$")) {
				r = 4;
			} else {
				r = -1;
			}
			r = r * 10;
			return Math.abs(r);
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
		System.out.println(code);

		assertThat(code, containsString("} else if (str.equals(\"b\")) {"));
		assertThat(code, containsString("} else {"));
		assertThat(code, containsString("int r;"));
		assertThat(code, containsString("r = 1;"));
		assertThat(code, containsString("r = -1;"));
		// no ternary operator
		assertThat(code, not(containsString("?")));
		assertThat(code, not(containsString(":")));
	}
}
