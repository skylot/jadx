package jadx.tests.integration.inline;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTernaryCast extends IntegrationTest {

	public static class TestCls {
		public String test(boolean b, Object obj, CharSequence cs) {
			return (String) (b ? obj : cs);
		}

		public void check() {
			assertThat(test(true, "a", "b")).isEqualTo("a");
			assertThat(test(false, "a", "b")).isEqualTo("b");
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("return (String) (b ? obj : cs);");
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		getClassNode(TestCls.class);
	}
}
