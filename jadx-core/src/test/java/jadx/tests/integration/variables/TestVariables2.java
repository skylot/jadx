package jadx.tests.integration.variables;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("Object store = s != null ? s : null;"));
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("Object obj2 = obj != null ? obj : null;"));
	}
}
