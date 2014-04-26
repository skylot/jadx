package jadx.tests.internal.synchronize;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class TestSynchronized2 extends InternalJadxTest {

	public static class TestCls {
		private static synchronized boolean test(Object obj) {
			return obj.toString() != null;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
		System.out.println(code);

		assertThat(code, containsString("private static synchronized boolean test(Object obj) {"));
		assertThat(code, containsString("obj.toString() != null;"));
		// TODO
//		assertThat(code, containsString("return obj.toString() != null;"));
//		assertThat(code, not(containsString("synchronized (")));
	}
}
