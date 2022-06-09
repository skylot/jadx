package jadx.tests.integration.invoke;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSuperInvokeUnknown extends IntegrationTest {

	public static class TestCls {
		public static class BaseClass {
			public int doSomething() {
				return 0;
			}
		}

		public static class NestedClass extends BaseClass {
			@Override
			public int doSomething() {
				return super.doSomething();
			}
		}
	}

	@Test
	public void test() {
		disableCompilation();
		noDebugInfo();
		assertThat(getClassNode(TestCls.NestedClass.class)) // BaseClass unknown
				.code()
				.containsOne("return super.doSomething();");
	}

	@Test
	public void testTopCls() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("return super.doSomething();");
	}
}
