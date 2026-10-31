package jadx.tests.integration.inner;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

/**
 * Own member classes are not in scope in the class header (JLS 6.3),
 * so they must keep the outer class name there.
 */
public class TestInnerClassInHeader extends IntegrationTest {

	public static class TestCls {
		static class Gen<T> {
		}

		public static class Outer extends Gen<Outer.Results> implements Comparable<Outer.A.B> {
			public static class Results {
			}

			public static class A {
				public static class B {
				}
			}

			@Override
			public int compareTo(A.B o) {
				return 0;
			}
		}

		public static class Bounded<T extends List<Bounded.Item>> {
			public static class Item {
			}
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("extends Gen<Outer.Results> implements Comparable<Outer.A.B> {")
				.containsOne("class Bounded<T extends List<Bounded.Item>> {");
	}

	@Test
	public void testJava() {
		useJavaInput();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("extends Gen<Outer.Results> implements Comparable<Outer.A.B> {")
				.containsOne("class Bounded<T extends List<Bounded.Item>> {");
	}
}
