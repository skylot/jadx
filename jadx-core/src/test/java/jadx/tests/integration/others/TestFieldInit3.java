package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestFieldInit3 extends IntegrationTest {

	public static class TestCls {

		public abstract static class A {
			public int field = 4;
		}

		public static final class B extends A {
			public B() {
				// IPUT for A.field
				super.field = 7;
			}
		}

		public static final class C extends A {
			public int other = 11;

			public C() {
				// IPUT for C.field not A.field !!!
				this.field = 9;
			}
		}

		public static final class D extends A {
		}

		public void check() {
			assertThat(new B().field, is(7));
			assertThat(new C().field, is(9));
			assertThat(new C().other, is(11));
			assertThat(new D().field, is(4));
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("public int field = 4;"));
		assertThat(code, containsOne("field = 7;"));
		assertThat(code, containsOne("field = 9;"));
		assertThat(code, containsOne("public int other = 11;"));
	}
}
