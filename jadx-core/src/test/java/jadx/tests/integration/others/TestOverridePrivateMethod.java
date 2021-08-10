package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestOverridePrivateMethod extends IntegrationTest {

	public static class TestCls {
		public static class BaseClass {
			private int a() {
				return 1;
			}
		}

		public static class MyClass extends BaseClass {
			public int a() {
				return 2;
			}
		}

		public void check() {
			assertThat(new MyClass().a()).isEqualTo(2);
			assertThat(new BaseClass().a()).isEqualTo(1);
			// TODO: assertThat(((BaseClass) new MyClass()).a()).isEqualTo(1);
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("@Override");
	}
}
