package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestAnonymousClass22 extends IntegrationTest {

	public static class TestCls {

		public static class Parent {
			public static Parent test(Class<?> cls) {
				final AnotherClass another = new AnotherClass();
				return new Parent() {
					@Override
					public String func() {
						return another.toString();
					}
				};
			}

			public String func() {
				return "";
			}
		}

		public static class AnotherClass {
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("return another.toString();")
				.doesNotContain("AnotherClass.this");
	}
}
