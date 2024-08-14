package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestInner2Samples extends IntegrationTest {

	public static class TestInner2 {
		private String a;

		public class A {
			public A() {
				a = "a";
			}

			public String a() {
				return a;
			}
		}

		private static String b;

		public static class B {
			public B() {
				b = "b";
			}

			public String b() {
				return b;
			}
		}

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

		private static String d;

		private static void setD(String s) {
			d = s;
		}

		public static class D {
			public String d() {
				setD("d");
				return d;
			}
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestInner2.class))
				.code()
				.containsOne("setD(\"d\");")
				.doesNotContain("synthetic")
				.doesNotContain("access$");
	}
}
