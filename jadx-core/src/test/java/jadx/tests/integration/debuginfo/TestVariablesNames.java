package jadx.tests.integration.debuginfo;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.JadxMatchers.containsLines;
import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertThat;

public class TestVariablesNames extends SmaliTest {
/*
	public static class TestCls {

		public void test(String s, int k) {
			f1(s);
			int i = k + 3;
			String s2 = "i" + i;
			f2(i, s2);
			double d = i * 5;
			String s3 = "d" + d;
			f3(d, s3);
		}

		private void f1(String s) {
		}

		private void f2(int i, String i2) {
		}

		private void f3(double d, String d2) {
		}
	}
*/

	/**
	 * Parameter register reused in variables assign with different types and names
	 * No variables names in debug info
	 */
	@Test
	public void test() {
		ClassNode cls = getClassNodeFromSmaliWithPath("debuginfo", "TestVariablesNames");
		String code = cls.getCode().toString();

		// TODO: don't use current variables naming in tests
		assertThat(code, containsOne("f1(str);"));
		assertThat(code, containsOne("f2(i2, \"i\" + i2);"));
		assertThat(code, containsOne("f3(d, \"d\" + d);"));
	}
}
