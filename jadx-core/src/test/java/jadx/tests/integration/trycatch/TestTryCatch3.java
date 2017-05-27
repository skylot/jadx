package jadx.tests.integration.trycatch;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TestTryCatch3 extends IntegrationTest {

	public static class TestCls {
		private int f = 0;

		private boolean test(Object obj) {
			boolean res;
			try {
				res = exc(obj);
			} catch (Exception e) {
				res = false;
			} finally {
				f++;
			}
			return res;
		}

		private boolean exc(Object obj) throws Exception {
			if ("r".equals(obj)) {
				throw new AssertionError();
			}
			return true;
		}

		public void check() {
			f = 0;
			assertTrue(test(null));
			assertEquals(1, f);

			f = 0;
			try {
				test("r");
			} catch (AssertionError e) {
				// pass
			}
			assertEquals(1, f);
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("try {"));
		assertThat(code, containsString("exc(obj);"));
		assertThat(code, containsString("} catch (Exception e) {"));

		assertThat(code, not(containsString("throw th;")));
	}

	@Test
	public void test2() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("throw th;")));
	}
}
