package jadx.tests.integration.inline;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSyntheticInline extends IntegrationTest {

	public static class TestCls {
		private int f;

		private int func() {
			return -1;
		}

		public class A {
			public int getF() {
				return f;
			}

			public void setF(int v) {
				f = v;
			}

			public int callFunc() {
				return func();
			}
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("synthetic")
				.doesNotContain("access$")
				.doesNotContain("x0")
				.contains("f = v;")
				.contains("return TestSyntheticInline$TestCls.this.f;")

				// .contains("return f;");
				// .contains("return func();");
				// Temporary solution
				.contains("return TestSyntheticInline$TestCls.this.func();");
	}
}
