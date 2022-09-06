package jadx.tests.integration.generics;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static jadx.tests.api.utils.JadxMatchers.countString;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestGenericsMthOverride extends IntegrationTest {

	public static class TestCls {
		public interface I<X, Y> {
			Y method(X x);
		}

		public static class A<X, Y> implements I<X, Y> {
			@Override
			public Y method(X x) {
				return null;
			}
		}

		public static class B<X, Y> implements I<X, Y> {
			@Override
			public Y method(Object x) {
				return null;
			}
		}

		public static class C<X extends Exception, Y> implements I<X, Y> {
			@Override
			public Y method(Exception x) {
				return null;
			}
		}

		@SuppressWarnings("unchecked")
		public static class D<X, Y> implements I<X, Y> {
			@Override
			public Object method(Object x) {
				return null;
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("public Y method(X x) {"));
		assertThat(code, containsOne("public Y method(Object x) {"));
		assertThat(code, containsOne("public Y method(Exception x) {"));
		assertThat(code, containsOne("public Object method(Object x) {"));

		assertThat(code, countString(4, "@Override"));
	}
}
