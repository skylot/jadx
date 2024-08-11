package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

public class TestTryCatchFinally7 extends IntegrationTest {

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
			assertThat(test(null)).isTrue();
			assertThat(f).isEqualTo(1);

			f = 0;
			try {
				test("r");
			} catch (AssertionError e) {
				// pass
			}
			assertThat(f).isEqualTo(1);
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("try {")
				.contains("exc(obj);")
				.contains("} catch (Exception e) {")
				.doesNotContain("throw th;");
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("throw th;");
	}
}
