package jadx.tests.internal.arith;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class TestFieldIncrement extends InternalJadxTest {

	public static class TestCls {
		public int instanceField = 1;
		public static int staticField = 1;
		public static String result = "";

		public void method() {
			instanceField++;
		}

		public void method2() {
			staticField--;
		}

		public void method3(String s) {
			result += s + '_';
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
		System.out.println(code);

		assertThat(code, containsString("instanceField++;"));
		assertThat(code, containsString("staticField--;"));
		assertThat(code, containsString("result += s + '_';"));
	}
}
