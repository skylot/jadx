package jadx.tests.integration.inline;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestInline6 extends IntegrationTest {

	public static class TestCls {
		public void f() {
		}

		public void test(int a, int b) {
			long start = System.nanoTime();
			f();
			System.out.println(System.nanoTime() - start);
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("System.out.println(System.nanoTime() - start);")
				.doesNotContain("System.out.println(System.nanoTime() - System.nanoTime());");
	}
}
