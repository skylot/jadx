package jadx.tests.integration.others;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static jadx.tests.api.utils.JadxMatchers.countString;
import static org.junit.Assert.assertThat;

public class TestIfTryInCatch extends IntegrationTest {

	public static class TestCls {
		private Exception exception;
		private java.lang.Object data;

		public java.lang.Object test(final Object obj) {
			exception = null;
			try {
				return f();
			} catch (Exception e) {
				if (a(e) && b(obj)) {
					try {
						return f();
					} catch (Exception e2) {
						e = e2;
					}
				}
				System.out.println("Exception" + e);
				exception = e;
				return data;
			}
		}

		private static boolean b(Object obj) {
			return obj == null;
		}

		private static boolean a(Exception e) {
			return e instanceof RuntimeException;
		}

		private Object f() {
			return null;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, countString(2, "try {"));
		assertThat(code, containsOne("if ("));
		assertThat(code, countString(2, "return f();"));
	}
}
