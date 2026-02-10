package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestBreakInLoop5 extends IntegrationTest {

	public static class TestCls {

		public long test(String spaceStr) {
			try {
				double multiplier = 1.0; // Often the compiler will move this line to each of the loop exits creating complexity
				char c;
				StringBuffer sb = new StringBuffer();
				for (int i = 0; i < spaceStr.length(); i++) {
					c = spaceStr.charAt(i);
					if (!Character.isDigit(c) && c != '.') {
						if (c == 'm' || c == 'M') {
							multiplier = 1024.0;
						} else if (c == 'g' || c == 'G') {
							multiplier = 1024.0 * 1024.0;
						}
						break;
					}
					sb.append(spaceStr.charAt(i));
				}
				return (long) Math.ceil(Double.valueOf(sb.toString()) * multiplier);
			} catch (Exception e) {
				return -1;
			}
		}

		public void check() {
			assertThat(test("1.2")).isEqualTo(2);
			assertThat(test("12am")).isEqualTo(12);
			assertThat(test("13m")).isEqualTo(13 * 1024);
			assertThat(test("1G4")).isEqualTo(1 * 1024 * 1024);
			assertThat(test("")).isEqualTo(-1);
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code();
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code();
	}
}
