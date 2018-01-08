package jadx.tests.integration;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestRedundantBrackets extends IntegrationTest {

	public static class TestCls {
		public boolean method(String str) {
			return str.indexOf('a') != -1;
		}

		public int method2(Object obj) {
			return obj instanceof String ? ((String) obj).length() : 0;
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

		public void method4(int num) {
			if (num == 4 || num == 6 || num == 8 || num == 10) {
				method2(null);
			}
		}

		public void method5(int a[], int n) {
			a[1] = n * 2;
			a[n - 1] = 1;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("(-1)")));
		assertThat(code, not(containsString("return;")));

		assertThat(code, containsString("return obj instanceof String ? ((String) obj).length() : 0;"));
		assertThat(code, containsString("a + b < 10"));
		assertThat(code, containsString("(a & b) != 0"));
		assertThat(code, containsString("if (num == 4 || num == 6 || num == 8 || num == 10)"));

		assertThat(code, containsString("a[1] = n * 2;"));
		assertThat(code, containsString("a[n - 1] = 1;"));

		// argument type not changed to String
		assertThat(code, containsString("public int method2(Object obj) {"));
		// cast not eliminated
		assertThat(code, containsString("((String) obj).length()"));
	}
}
