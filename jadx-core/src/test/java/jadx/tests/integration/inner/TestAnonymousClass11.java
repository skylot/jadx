package jadx.tests.integration.inner;

import java.util.Random;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("System.out.println(\"a\" + a);"));
		assertThat(code, containsOne("print(a);"));
		assertThat(code, not(containsString("synthetic")));
	}
}
