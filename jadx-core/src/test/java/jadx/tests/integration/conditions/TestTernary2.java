package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestTernary2 extends IntegrationTest {

	public static class TestCls {

		public void test() {
			assertTrue(f(1, 0) == 0);
		}

		private int f(int a, int b) {
			return a + b;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertEquals(1, count(code, "assertTrue"));
		assertEquals(1, count(code, "f(1, 0)"));
		// TODO:
//		assertThat(code, containsString("assertTrue(f(1, 0) == 0);"));
	}
}
