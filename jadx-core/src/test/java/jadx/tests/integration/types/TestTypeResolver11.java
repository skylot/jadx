package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.tests.api.IntegrationTest;

public class TestTypeResolver11 extends IntegrationTest {

	public static class TestCls {
		public Void test(Object... objArr) {
			int val = (Integer) objArr[0];
			String str = (String) objArr[1];
			call(str, str, val, val);
			return null;
		}

		private void call(String a, String b, int... val) {
		}

		public void check() {
			test(1, "str");
		}
	}

	@NotYetImplemented("Missing cast")
	@Test
	public void test() {
		getClassNode(TestCls.class);
	}

	@NotYetImplemented("Missing cast")
	@Test
	public void testNoDebug() {
		noDebugInfo();
		getClassNode(TestCls.class);
	}
}
