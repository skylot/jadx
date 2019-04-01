package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("super();")));
		assertThat(code, not(containsString("static {")));
		assertThat(code, containsOne("public B() {"));
	}
}
