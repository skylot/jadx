package jadx.tests.integration.trycatch;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class TestTryCatch3 extends IntegrationTest {

	public static class TestCls {
		private final static Object obj = new Object();
		private boolean mDiscovering;

		private boolean test(Object obj) {
			this.mDiscovering = false;
			try {
				exc(obj);
			} catch (Exception e) {
				e.toString();
			} finally {
				mDiscovering = true;
			}
			return mDiscovering;
		}

		private void exc(Object obj) throws Exception {
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("try {"));
		assertThat(code, containsString("exc(obj);"));
		assertThat(code, containsString("} catch (Exception e) {"));
	}
}
