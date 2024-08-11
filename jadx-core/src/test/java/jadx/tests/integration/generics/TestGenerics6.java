package jadx.tests.integration.generics;

import java.util.Collection;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

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

		private interface I {
			void f();
		}

		private class A implements I {
			public void f() {
			}
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("for (A a : as) {")
				.containsOne("for (I i : is) {");
	}
}
