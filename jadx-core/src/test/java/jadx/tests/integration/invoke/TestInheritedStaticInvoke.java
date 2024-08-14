package jadx.tests.integration.invoke;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestInheritedStaticInvoke extends IntegrationTest {

	public static class TestCls {
		public static class A {
			public static int a() {
				return 1;
			}
		}

		public static class B extends A {
		}

		public int test() {
			return B.a(); // not A.a()
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("return B.a();");
	}
}
