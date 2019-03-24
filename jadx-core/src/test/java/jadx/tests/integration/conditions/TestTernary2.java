package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertEquals(1, count(code, "f(1, 0)"));
	}

	@Test
	@NotYetImplemented
	public void test2() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("assertTrue(f(1, 0) == 0);"));
	}
}
