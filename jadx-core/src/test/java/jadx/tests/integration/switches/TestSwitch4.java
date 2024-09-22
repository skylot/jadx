package jadx.tests.integration.switches;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSwitch4 extends IntegrationTest {

	public static class TestCls {
		@SuppressWarnings({ "FallThrough", "unused" })
		private static int parse(char[] ch, int off, int len) {
			int num = ch[off + len - 1] - '0';
			switch (len) {
				case 4:
					num += (ch[off++] - '0') * 1000;
				case 3:
					num += (ch[off++] - '0') * 100;
				case 2:
					num += (ch[off] - '0') * 10;
			}
			return num;
		}

		public void check() {
			assertThat(parse("123".toCharArray(), 0, 3)).isEqualTo(123);
			assertThat(parse("a=1234".toCharArray(), 2, 4)).isEqualTo(1234);
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("switch (")
				.countString(3, "case ")
				.doesNotContain("break");
	}
}
