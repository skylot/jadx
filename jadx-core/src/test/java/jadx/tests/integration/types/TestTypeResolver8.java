package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestTypeResolver8 extends SmaliTest {

	// @formatter:off
	/*
		public class A {}

		public class B {
			public B(A a) {
			}
		}

		public static class TestCls {
			private A f;

			public void test() {
				A x = this.f;
				if (x != null) {
					x = new B(x); // different types, type of 'x' can't be resolved
				}
				use(x);
			}

			private void use(B b) {}
		}
	*/
	// @formatter:on

	@Test
	@NotYetImplemented
	public void test() {
		ClassNode cls = getClassNodeFromSmaliFiles("types", "TestTypeResolver8", "TestCls");
		String code = cls.getCode().toString();

		assertThat(code, containsOne("use(a != null ? new B(a) : null);"));
	}
}
