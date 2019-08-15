package jadx.tests.integration.generics;

import java.util.Set;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestOuterGeneric extends IntegrationTest {

	public static class TestCls {
		public static class A<T> {
			public class B<V> {
			}

			public class C {
			}
		}

		public static class D {
			public class E {
			}
		}

		public void test1() {
			A<String> a = new A<>();
			use(a);
			A<String>.B<Exception> b = a.new B<Exception>();
			use(b);
			use(b);
			A<String>.C c = a.new C();
			use(c);
			use(c);

			use(new A<Set<String>>().new C());
		}

		public void test2() {
			D d = new D();
			D.E e = d.new E();
			use(e);
			use(e);
		}

		public void test3() {
			use(A.class);
			use(A.B.class);
			use(A.C.class);
		}

		private void use(Object obj) {
		}
	}

	@NotYetImplemented("Instance constructor for inner classes")
	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("A<String> a = new A<>();"));
		assertThat(code, containsOne("A<String>.B<Exception> b = a.new B<Exception>();"));
		assertThat(code, containsOne("A<String>.C c = a.new C();"));
		assertThat(code, containsOne("use(new A<Set<String>>().new C());"));
		assertThat(code, containsOne("D.E e = d.new E();"));
	}
}
