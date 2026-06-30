package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestBreakInLoop4 extends IntegrationTest {

	public static class TestCls {

		public double test(char c) {
			double m = 1.0;
			for (int i = 0; i < 5; i++) {
				if (c != '.') {
					if (c == 'a' || c == 'b') {
						m = 1024.0;
					}
					break;
				}
			}
			return m;
		}

		public void check() {
			assertThat(test('.')).isEqualTo(1.0);
			assertThat(test('a')).isEqualTo(1024.0);
			assertThat(test('b')).isEqualTo(1024.0);
			assertThat(test('c')).isEqualTo(1.0);
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("while")
				.containsOne("for");
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("while")
				.containsOne("for");
	}
}
