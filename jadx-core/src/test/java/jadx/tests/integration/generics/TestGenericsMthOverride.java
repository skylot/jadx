package jadx.tests.integration.generics;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

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
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("public Y method(X x) {")
				.containsOne("public Y method(Object x) {")
				.containsOne("public Y method(Exception x) {")
				.containsOne("public Object method(Object x) {")
				.countString(4, "@Override");
	}
}
