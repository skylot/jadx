package jadx.tests.integration.generics;

import java.util.Collection;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertThat;

public class TestGenerics6 extends IntegrationTest {

	public static class TestCls {
		public void test1(Collection<? extends A> as) {
			for (A a : as) {
				a.f();
			}
		}

		public void test2(Collection<? extends A> is) {
			for (I i : is) {
				i.f();
			}
		}

		private interface  I {
			void f();
		}

		private class A implements I {
			public void f() {
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("for (A a : as) {"));
		// TODO: fix iterable arg type (unexpected cast to A in bytecode)
//		assertThat(code, containsOne("for (I i : is) {"));
	}
}
