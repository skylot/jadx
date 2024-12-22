package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestGenerics8 extends IntegrationTest {

	public static class TestCls<T> {

		public abstract static class Class2<S extends I1 & I2> extends Parent2<S> {
			public void test() {
				S s = get();
				s.i1();
				s.i2();
			}
		}

		static class Parent2<T extends I1> {
			T t;

			protected T get() {
				return t;
			}
		}

		interface I1 {
			void i1();
		}

		interface I2 {
			void i2();
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("S s = get();")
				.containsOne("s.i1();")
				.containsOne("s.i2();");
	}
}
