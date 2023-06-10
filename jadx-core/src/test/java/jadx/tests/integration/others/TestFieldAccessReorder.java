package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestFieldAccessReorder extends IntegrationTest {
	public static class TestCls {
		private long field = 10;

		public final boolean test() {
			long value = longCall();
			long diff = value - this.field;
			this.field = value;
			return diff > 250;
		}

		public static long longCall() {
			return 261L;
		}

		public void check() {
			assertTrue(test());
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		getClassNode(TestCls.class);
		// auto check should pass
	}
}
