package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestStaticMethod extends IntegrationTest {

	public static class TestCls {
		static {
			f();
		}

		private static void f() {
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("static {")
				.contains("private static void f() {");
	}
}
