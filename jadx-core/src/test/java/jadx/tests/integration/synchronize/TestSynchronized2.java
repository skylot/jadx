package jadx.tests.integration.synchronize;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class TestSynchronized2 extends IntegrationTest {

	public static class TestCls {
		private static synchronized boolean test(Object obj) {
			return obj.toString() != null;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("private static synchronized boolean test(Object obj) {"));
		assertThat(code, containsString("obj.toString() != null;"));
		// TODO
//		assertThat(code, containsString("return obj.toString() != null;"));
//		assertThat(code, not(containsString("synchronized (")));
	}
}
