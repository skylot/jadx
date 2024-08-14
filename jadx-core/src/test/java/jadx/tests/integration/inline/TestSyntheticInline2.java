package jadx.tests.integration.inline;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSyntheticInline2 extends IntegrationTest {

	public static class Base {
		protected void call() {
			System.out.println("base call");
		}
	}

	public static class TestCls extends Base {
		public class A {
			public void invokeCall() {
				TestCls.this.call();
			}

			public void invokeSuperCall() {
				TestCls.super.call();
			}
		}

		@Override
		public void call() {
			System.out.println("TestCls call");
		}

		public void check() {
			A a = new A();
			a.invokeSuperCall();
			a.invokeCall();
		}
	}

	@Test
	public void test() {
		disableCompilation(); // strange java compiler bug
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("synthetic")
				.doesNotContain("access$").contains("TestSyntheticInline2$TestCls.this.call();")
				.contains("TestSyntheticInline2$TestCls.super.call();");
	}

	@Test
	public void testTopClass() {
		JadxAssertions.assertThat(getClassNode(TestSyntheticInline2.class))
				.code()
				.contains(indent(1) + "TestCls.super.call();");
	}
}
