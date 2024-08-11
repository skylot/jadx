package jadx.tests.integration.annotations;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestAnnotations extends IntegrationTest {

	public static class TestCls {
		private @interface A {
			int a();
		}

		@A(a = -1)
		public void methodA1() {
		}

		@A(a = -253)
		public void methodA2() {
		}

		@A(a = -11253)
		public void methodA3() {
		}

		private @interface V {
			boolean value();
		}

		@V(false)
		public void methodV() {
		}

		private @interface D {
			float value() default 1.1f;
		}

		@D
		public void methodD() {
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class)).code()
				.doesNotContain("@A(a = 255)")
				.containsOne("@A(a = -1)")
				.containsOne("@A(a = -253)")
				.containsOne("@A(a = -11253)")
				.containsOne("@V(false)")
				.doesNotContain("@D()")
				.containsOne("@D")
				.containsOne("int a();")
				.containsOne("float value() default 1.1f;");
	}
}
