package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestGenerics extends IntegrationTest {

	public static class TestCls<T> {
		public T data;

		public TestCls<T> data(T t) {
			this.data = t;
			return this;
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("TestCls<T> data(T t) {");
	}

	@Test
	public void test2() {
		noDebugInfo();
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("TestCls<T> data(T t) {");
	}
}
