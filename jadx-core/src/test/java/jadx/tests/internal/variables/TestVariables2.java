package jadx.tests.internal.variables;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class TestVariables2 extends InternalJadxTest {

	public static class TestCls {
		Object test(Object s) {
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
		System.out.println(code);

		assertThat(code, containsString("Object store = s != null ? s : null;"));
	}
}
