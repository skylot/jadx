package jadx.tests.integration.generics;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestGenerics4 extends IntegrationTest {

	public static class TestCls {

		public static Class<?> method(int i) {
			Class<?>[] a = new Class<?>[0];
			return a[a.length - i];
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("Class<?>[] a =")
				.doesNotContain("Class[] a =");
	}
}
