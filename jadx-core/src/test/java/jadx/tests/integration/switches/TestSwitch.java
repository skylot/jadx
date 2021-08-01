package jadx.tests.integration.switches;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestSwitch extends IntegrationTest {
	public static class TestCls {
		public String test(String str) {
			int len = str.length();
			StringBuilder sb = new StringBuilder(len);
			for (int i = 0; i < len; i++) {
				char c = str.charAt(i);
				switch (c) {
					case '.':
					case '/':
						sb.append('_');
						break;

					case ']':
						sb.append('A');
						break;

					case '?':
						break;

					default:
						sb.append(c);
						break;
				}
			}
			return sb.toString();
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("case '/':"));
		assertThat(code, containsString(indent(5) + "break;"));
		assertThat(code, containsString(indent(4) + "default:"));

		assertEquals(1, count(code, "i++"));
		assertEquals(4, count(code, "break;"));
	}
}
