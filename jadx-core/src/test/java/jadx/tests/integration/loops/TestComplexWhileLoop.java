package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestComplexWhileLoop extends IntegrationTest {

	public static class TestCls {
		public static String test(String[] arr) {
			int index = 0;
			int length = arr.length;
			String str;
			while ((str = arr[index]) != null) {
				if (str.length() == 1) {
					return str.trim();
				}
				if (++index >= length) {
					index = 0;
				}
			}
			System.out.println("loop end");
			return "";
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("for (int at = 0; at < len; at = endAt) {")));
	}
}
