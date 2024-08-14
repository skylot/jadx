package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestDefConstructorNotRemoved extends IntegrationTest {

	public static class TestCls {

		static {
			// empty
		}

		public static class A {
			public final String s;

			public A() {
				s = "a";
			}

			public A(String str) {
				s = str;
			}
		}

		public static class B extends A {
			public B() {
				super();
			}

			public B(String s) {
				super(s);
			}
		}

		public void check() {
			new A();
			new A("a");
			new B();
			new B("b");
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("super();")
				.doesNotContain("static {")
				.containsOne("public B() {");
	}
}
