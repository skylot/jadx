package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestOverrideStaticMethod extends IntegrationTest {

	public static class TestCls {
		public static class BaseClass {
			public static int a() {
				return 1;
			}
		}

		public static class MyClass extends BaseClass {
			public static int a() {
				return 2;
			}
		}

		public void check() {
			assertThat(BaseClass.a()).isEqualTo(1);
			assertThat(MyClass.a()).isEqualTo(2);
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("@Override");
	}
}
