package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestInnerConstructorCall extends IntegrationTest {

	public static class TestCls {
		@SuppressWarnings("InnerClassMayBeStatic")
		public class A {
			public class AA {
				public void test() {
				}
			}
		}

		public void test() {
			A a = new A();
			A.AA aa = a.new AA();
			aa.test();
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("A.AA aa = a.new AA();");
	}
}
