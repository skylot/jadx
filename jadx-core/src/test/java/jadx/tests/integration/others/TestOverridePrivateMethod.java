package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestOverridePrivateMethod extends IntegrationTest {

	public static class TestCls {
		public static class BaseClass {
			private void a() {
			}
		}

		public static class MyClass extends BaseClass {
			public void a() {
			}
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("@Override");
	}
}
