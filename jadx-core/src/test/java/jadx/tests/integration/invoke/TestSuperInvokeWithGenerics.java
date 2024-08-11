package jadx.tests.integration.invoke;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

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
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("super(e);")
				.containsOne("super(s);");
	}
}
