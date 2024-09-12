package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.RaungTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestClassImplementsSignature extends RaungTest {

	public static class TestCls {
		public abstract static class A<T> implements Comparable<A<T>> {
			T value;
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("public static abstract class A<T> implements Comparable<A<T>> {");
	}

	@Test
	public void testRaung() {
		allowWarnInCode();
		assertThat(getClassNodeFromRaung())
				.code()
				.containsOne("public class TestClassImplementsSignature<T> {")
				.containsOne("Unexpected interfaces in signature");
	}
}
