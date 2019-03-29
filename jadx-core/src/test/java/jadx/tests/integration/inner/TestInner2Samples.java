package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

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
		ClassNode cls = getClassNode(TestInner2.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("setD(\"d\");"));
		assertThat(code, not(containsString("synthetic")));
		assertThat(code, not(containsString("access$")));
	}
}
