package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

import static org.assertj.core.api.Assertions.assertThat;

public class TestNestedLoops3 extends IntegrationTest {

	public static class TestCls {
		int c = 0;

		public int test(int b) {
			int i;
			loop0: while (true) {
				f1();
				i = 0;
				while (true) {
					f2();
					if (i != 0) {
						break loop0;
					}
					i += 3;
					if (b >= 16) {
						break loop0;
					}
					try {
						exc();
						break;
					} catch (Exception e) {
						// ignore
					}
				}
			}
			return i;
		}

		private void exc() throws Exception {
			if (c > 200) {
				throw new Exception();
			}
		}

		private void f1() {
			c += 1;
		}

		private void f2() {
			c += 100;
		}

		public void check() {
			test(1);
			assertThat(c).isEqualTo(302);
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("} catch (Exception e) {");
	}
}
