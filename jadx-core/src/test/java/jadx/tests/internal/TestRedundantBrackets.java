package jadx.tests.internal;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class TestRedundantBrackets extends InternalJadxTest {

	public static class TestCls {
		public boolean method(String str) {
			return str.indexOf('a') != -1;
		}

		public int method2(Object obj) {
			if (obj instanceof String) {
				return ((String) obj).length();
			}
			return 0;
		}

		public int method3(int a, int b) {
			if (a + b < 10) {
				return a;
			}
			if ((a & b) != 0) {
				return a * b;
			}
			return b;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);

		String code = cls.getCode().toString();
//		assertThat(code, not(containsString("(-1)")));
		assertThat(code, containsString("if (obj instanceof String)"));
		assertThat(code, containsString("if (a + b < 10)"));
		assertThat(code, containsString("if ((a & b) != 0)"));
	}
}
