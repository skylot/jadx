package jadx.tests.integration.inner;

import java.util.Random;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestAnonymousClass11 extends IntegrationTest {

	public static class TestCls {

		public void test() {
			final int a = new Random().nextInt();
			final long l = new Random().nextLong();
			func(new A(l) {
				@Override
				public void m() {
					System.out.println(a);
				}
			});
			System.out.println("a" + a);
			print(a);
			print2(1, a);
			print3(1, l);
		}

		public abstract class A {
			public A(long l) {
			}

			public abstract void m();
		}

		private void func(A a) {
		}

		private void print(int a) {
		}

		private void print2(int i, int a) {
		}

		private void print3(int i, long l) {
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("System.out.println(\"a\" + a);")
				.containsOne("print(a);")
				.doesNotContain("synthetic");
	}
}
