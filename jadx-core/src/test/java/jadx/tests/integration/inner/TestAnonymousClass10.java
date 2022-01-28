package jadx.tests.integration.inner;

import java.util.Random;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("return new A(this, a2, a2 + 3, 4, 5, random.nextDouble()) {"));
		assertThat(code, not(containsString("synthetic")));
	}
}
