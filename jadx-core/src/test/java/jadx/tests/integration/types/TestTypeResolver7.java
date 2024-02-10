package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTypeResolver7 extends IntegrationTest {

	public static class TestCls {
		public void test(boolean a, boolean b) {
			Object obj = null;
			if (a) {
				use(b ? (Exception) getObj() : (Exception) obj);
			} else {
				Runnable r = (Runnable) obj;
				if (b) {
					r = (Runnable) getObj();
				}
				use(r);
			}
		}

		private Object getObj() {
			return null;
		}

		private void use(Exception e) {
		}

		private void use(Runnable r) {
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.oneOf(c -> c.containsOne("use(b ? (Exception) getObj() : null);"),
						c -> c.containsOne("use(b ? (Exception) getObj() : (Exception) null);"));
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		getClassNode(TestCls.class);
	}
}
