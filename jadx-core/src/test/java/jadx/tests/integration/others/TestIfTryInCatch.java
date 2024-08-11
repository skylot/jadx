package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestIfTryInCatch extends IntegrationTest {

	public static class TestCls {
		public Exception exception;
		private java.lang.Object data;

		public java.lang.Object test(final Object obj) {
			exception = null;
			try {
				return f();
			} catch (Exception e) {
				if (a(e) && b(obj)) {
					try {
						return f();
					} catch (Exception exc) {
						e = exc;
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
		assertThat(getClassNode(TestCls.class))
				.code()
				.countString(2, "try {")
				.containsOne("if (")
				.countString(2, "return f();");
	}
}
