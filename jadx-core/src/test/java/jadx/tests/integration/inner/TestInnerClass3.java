package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestInnerClass3 extends IntegrationTest {

	public static class TestCls {
		private String c;

		private void setC(String c) {
			this.c = c;
		}

		public class C {
			public String c() {
				setC("c");
				return c;
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
				.contains("setC(\"c\");");
	}
}
