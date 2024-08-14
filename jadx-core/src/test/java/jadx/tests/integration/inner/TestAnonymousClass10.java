package jadx.tests.integration.inner;

import java.util.Random;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestAnonymousClass10 extends IntegrationTest {

	public static class TestCls {

		public A test() {
			Random random = new Random();
			int a2 = random.nextInt();
			return new A(this, a2, a2 + 3, 4, 5, random.nextDouble()) {
				@Override
				public void m() {
					System.out.println(1);
				}
			};
		}

		public abstract class A {
			public A(TestCls a1, int a2, int a3, int a4, int a5, double a6) {
			}

			public abstract void m();
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("return new A(this, a2, a2 + 3, 4, 5, random.nextDouble()) {")
				.doesNotContain("synthetic");
	}
}
