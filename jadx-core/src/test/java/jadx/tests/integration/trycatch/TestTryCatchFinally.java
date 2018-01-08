package jadx.tests.integration.trycatch;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TestTryCatchFinally extends IntegrationTest {

	public static class TestCls {
		public boolean f;

		private boolean test(Object obj) {
			this.f = false;
			try {
				exc(obj);
			} catch (Exception e) {
				e.getMessage();
			} finally {
				f = true;
			}
			return f;
		}

		private static boolean exc(Object obj) throws Exception {
			if (obj == null) {
				throw new Exception("test");
			}
			return (obj instanceof String);
		}

		public void check() {
			assertTrue(test("a"));
			assertTrue(test(null));
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("exc(obj);"));
		assertThat(code, containsOne("} catch (Exception e) {"));
		assertThat(code, containsOne("e.getMessage();"));
		assertThat(code, containsOne("} finally {"));
		assertThat(code, containsOne("f = true;"));
		assertThat(code, containsOne("return this.f;"));
	}
}
