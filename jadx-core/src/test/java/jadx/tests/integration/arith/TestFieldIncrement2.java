package jadx.tests.integration.arith;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestFieldIncrement2 extends IntegrationTest {

	public static class TestCls {
		private static class A {
			int f = 5;
		}

		public A a;

		public void test1(int n) {
			this.a.f = this.a.f + n;
		}

		public void test2(int n) {
			this.a.f *= n;
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("this.a.f += n;")
				.contains("this.a.f *= n;");
	}
}
