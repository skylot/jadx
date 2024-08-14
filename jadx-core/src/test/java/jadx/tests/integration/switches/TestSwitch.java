package jadx.tests.integration.switches;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

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
		assertThat(getClassNode(TestCls.class))
				.code()
				.contains("case '/':")
				.contains(indent(5) + "break;")
				.contains(indent(4) + "default:")
				.containsOne("i++")
				.countString(4, "break;");
	}
}
