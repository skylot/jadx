package jadx.tests.integration.names;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestSameMethodsNames extends IntegrationTest {

	public static class TestCls<V> {

		public static void test() {
			new Bug().Bug();
		}

		public static class Bug {
			public Bug() {
				System.out.println("constructor");
			}

			@SuppressWarnings({ "MethodName", "MethodNameSameAsClassName" })
			void Bug() {
				System.out.println("Bug");
			}
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("new Bug().Bug();");
	}
}
