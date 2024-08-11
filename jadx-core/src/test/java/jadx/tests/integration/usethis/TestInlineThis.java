package jadx.tests.integration.usethis;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestInlineThis extends IntegrationTest {

	public static class TestCls {
		public int field;

		public void test() {
			TestCls something = this;
			something.method();
			something.field = 123;
		}

		private void method() {
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("something")
				.doesNotContain("something.method()")
				.doesNotContain("something.field")
				.doesNotContain("= this")
				.containsOne("this.field = 123;")
				.containsOne("method();");
	}
}
