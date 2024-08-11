package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestTypeResolver extends IntegrationTest {

	public static class TestCls {
		public TestCls(int b1, int b2) {
			// test 'this' move and constructor invocation on moved register
			this(b1, b2, 0, 0, 0);
		}

		public TestCls(int a1, int a2, int a3, int a4, int a5) {
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("this(b1, b2, 0, 0, 0);")
				.doesNotContain("= this;");
	}
}
