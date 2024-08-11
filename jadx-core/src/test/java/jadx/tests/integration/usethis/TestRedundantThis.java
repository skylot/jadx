package jadx.tests.integration.usethis;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestRedundantThis extends IntegrationTest {

	public static class TestCls {
		public int field1 = 1;
		public int field2 = 2;

		public boolean f1() {
			return false;
		}

		public int method() {
			f1();
			return field1;
		}

		public void method2(int field2) {
			this.field2 = field2;
		}
	}

	// @Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("this.f1();")
				.doesNotContain("return this.field1;")
				.contains("this.field2 = field2;");
	}
}
