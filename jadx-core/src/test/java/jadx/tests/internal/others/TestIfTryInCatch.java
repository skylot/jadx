package jadx.tests.internal.others;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static jadx.tests.utils.JadxMatchers.containsOne;
import static jadx.tests.utils.JadxMatchers.countString;
import static org.junit.Assert.assertThat;

public class TestIfTryInCatch extends InternalJadxTest {

	public static class TestCls {
		private static final String TAG = "TAG";
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
			return false;
		}

		private static boolean a(Exception e) {
			return false;
		}

		private Object f() {
			return null;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
		System.out.println(code);

		assertThat(code, countString(2, "try {"));
		assertThat(code, containsOne("if ("));
		assertThat(code, countString(2, "return f();"));
	}
}
