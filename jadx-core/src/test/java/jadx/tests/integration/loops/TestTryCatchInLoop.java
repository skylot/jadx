package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

import static org.assertj.core.api.Assertions.assertThat;

public class TestTryCatchInLoop extends IntegrationTest {

	public static class TestCls {
		int c = 0;

		public int test() {
			while (true) {
				try {
					exc();
					break;
				} catch (Exception e) {
					//
				}
			}
			if (c == 5) {
				System.out.println(c);
			}
			return 0;
		}

		private void exc() throws Exception {
			c++;
			if (c < 3) {
				throw new Exception();
			}
		}

		public void check() {
			test();
			assertThat(c).isEqualTo(3);
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("} catch (Exception e) {")
				.containsOne("break;");
	}
}
