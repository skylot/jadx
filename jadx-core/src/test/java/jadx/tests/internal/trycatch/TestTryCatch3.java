package jadx.tests.internal.trycatch;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class TestTryCatch3 extends InternalJadxTest {

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
		System.out.println(code);

		assertThat(code, containsString("try {"));
		assertThat(code, containsString("exc(obj);"));
		assertThat(code, containsString("} catch (Exception e) {"));
	}
}
