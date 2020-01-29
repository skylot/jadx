package jadx.tests.integration.invoke;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestSuperInvokeWithGenerics extends IntegrationTest {

	public static class TestCls {

		public static class A<T extends Exception, V> {
			public A(T t) {
				System.out.println("t" + t);
			}

			public A(V v) {
				System.out.println("v" + v);
			}
		}

		public static class B extends A<Exception, String> {
			public B(String s) {
				super(s);
			}

			public B(Exception e) {
				super(e);
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("super(e);"));
		assertThat(code, containsOne("super(s);"));
	}
}
