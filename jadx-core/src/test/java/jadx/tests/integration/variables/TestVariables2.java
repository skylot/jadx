package jadx.tests.integration.variables;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestVariables2 extends IntegrationTest {

	public static class TestCls {
		public Object test(Object s) {
			Object store = s != null ? s : null;
			if (store == null) {
				store = new Object();
				s = store;
			}
			return store;
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("Object store = s != null ? s : null;");
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("Object obj2 = obj != null ? obj : null;");
	}
}
