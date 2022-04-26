package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSequentialLoops extends IntegrationTest {

	public static class TestCls {
		public int test(int a, int b) {
			int c = b;
			int z;

			while (true) {
				z = c + a;
				if (z >= 7) {
					break;
				}
				c = z;
			}

			while ((z = c + a) >= 7) {
				c = z;
			}
			return c;
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.countString(2, "while (")
				.containsOne("break;")
				.containsOne("return c;");
	}
}
