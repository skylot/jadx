package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

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
			assertThat(new B().field).isEqualTo(7);
			assertThat(new C().field).isEqualTo(9);
			assertThat(new C().other).isEqualTo(11);
			assertThat(new D().field).isEqualTo(4);
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("public int field = 4;")
				.containsOne("field = 7;")
				.containsOne("field = 9;")
				.containsOne("public int other = 11;");
	}
}
