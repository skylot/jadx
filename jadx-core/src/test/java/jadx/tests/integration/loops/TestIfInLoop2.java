package jadx.tests.integration.loops;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestIfInLoop2 extends IntegrationTest {

	public static class TestCls {
		public static void test(String str) {
			int len = str.length();
			int at = 0;
			while (at < len) {
				char c = str.charAt(at);
				int endAt = at + 1;
				if (c == 'A') {
					while (endAt < len) {
						c = str.charAt(endAt);
						if (c == 'B') {
							break;
						}
						endAt++;
					}
				}
				at = endAt;
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("for (int at = 0; at < len; at = endAt) {")));
	}
}
