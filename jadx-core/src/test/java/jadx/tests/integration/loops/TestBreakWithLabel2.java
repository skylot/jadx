package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 'continue' to the outer loop from the innermost of three nested loops
 * is decompiled as 'break' from the middle loop, so it must be labeled
 */
public class TestBreakWithLabel2 extends IntegrationTest {

	public static class TestCls {
		public int testFor(int n) {
			int c = 0;
			outer: for (int i = 0; i < n; i++) {
				for (int j = 0; j < n; j++) {
					for (int k = 0; k < n; k++) {
						if (i + j + k == 3) {
							continue outer;
						}
						c++;
					}
				}
			}
			return c;
		}

		public int testWhile(int n) {
			int c = 0;
			int i = 0;
			outer: while (i < n) {
				i++;
				int j = 0;
				while (j < n) {
					j++;
					int k = 0;
					while (k < n) {
						k++;
						if (i + j + k == 4) {
							continue outer;
						}
						c++;
					}
				}
			}
			return c;
		}

		public void check() {
			assertThat(testFor(3)).isEqualTo(8);
			assertThat(testFor(4)).isEqualTo(6);
			assertThat(testFor(5)).isEqualTo(31);
			assertThat(testWhile(3)).isEqualTo(10);
			assertThat(testWhile(4)).isEqualTo(33);
			assertThat(testWhile(5)).isEqualTo(76);
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.countString(2, "break loop");
	}

	@Test
	public void testJava() {
		useJavaInput();
		assertThat(getClassNode(TestCls.class))
				.code()
				.countString(2, "break loop");
	}
}
