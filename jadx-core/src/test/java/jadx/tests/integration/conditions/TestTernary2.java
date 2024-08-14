package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTernary2 extends IntegrationTest {

	public static class TestCls {

		public void test() {
			checkFalse(f(1, 0) == 0);
		}

		private int f(int a, int b) {
			return a + b;
		}

		private void checkFalse(boolean b) {
			if (b) {
				throw new AssertionError("Must be false");
			}
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("f(1, 0)");
	}

	@Test
	@NotYetImplemented
	public void test2() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.contains("assertTrue(f(1, 0) == 0);");
	}
}
