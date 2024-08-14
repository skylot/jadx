package jadx.tests.integration.inline;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestInline3 extends IntegrationTest {

	public static class TestCls {
		public TestCls(int b1, int b2) {
			this(b1, b2, 0, 0, 0);
		}

		public TestCls(int a1, int a2, int a3, int a4, int a5) {
		}

		public class A extends TestCls {
			public A(int a) {
				super(a, a);
			}
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("this(b1, b2, 0, 0, 0);")
				.contains("super(a, a);")
				.doesNotContain("super(a, a).this$0")
				.contains("public class A extends TestInline3$TestCls {");
	}
}
