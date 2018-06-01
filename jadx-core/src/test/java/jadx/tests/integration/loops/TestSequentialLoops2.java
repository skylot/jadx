package jadx.tests.integration.loops;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static jadx.tests.api.utils.JadxMatchers.countString;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class TestSequentialLoops2 extends IntegrationTest {

	public static class TestCls {
		private static char[] lowercases = new char[]{'a'};

		public static String asciiToLowerCase(String s) {
			char[] c = null;
			int i = s.length();
			while (i-- > 0) {
				char c1 = s.charAt(i);
				if (c1 <= 127) {
					char c2 = lowercases[c1];
					if (c1 != c2) {
						c = s.toCharArray();
						c[i] = c2;
						break;
					}
				}
			}
			while (i-- > 0) {
				if (c[i] <= 127) {
					c[i] = lowercases[c[i]];
				}
			}
			return c == null ? s : new String(c);
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, countString(2, "while ("));
		assertThat(code, containsString("break;"));
		assertThat(code, containsOne("return c"));
		assertThat(code, countString(2, "<= 127"));
	}
}
