package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

/**
 * Issue https://github.com/skylot/jadx/issues/956
 */
public class TestGenerics7 extends IntegrationTest {

	public static class TestCls<T> {
		private Object[] elements = new Object[1];

		@SuppressWarnings("unchecked")
		public final T test(int i) {
			Object[] arr = this.elements;
			T obj = (T) arr[i];
			arr[i] = null;
			if (obj == null) {
				throw new NullPointerException();
			}
			return obj;
		}

		public void check() {
			this.elements = new Object[] { 1, "" };
			assertThat(test(1)).isEqualTo("");
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("T t = (T) objArr[i];");
	}
}
