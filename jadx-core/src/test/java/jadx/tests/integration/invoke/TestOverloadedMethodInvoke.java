package jadx.tests.integration.invoke;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

import static org.assertj.core.api.Assertions.assertThat;

public class TestOverloadedMethodInvoke extends IntegrationTest {

	public static class TestCls {
		int c;

		public void method(Throwable th, int a) {
			c++;
			if (th != null) {
				c += 100;
			}
			c += a;
		}

		public void method(Exception e, int a) {
			c += 1000;
			if (e != null) {
				c += 10000;
			}
			c += a;
		}

		public void test(Throwable th, Exception e) {
			method(e, 10);
			method(th, 100);
			method((Throwable) e, 1000);
			method((Exception) th, 10000);
		}

		public void check() {
			test(null, new Exception());
			assertThat(c).isEqualTo(23212);
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("public void test(Throwable th, Exception e) {")
				.containsOne("method(e, 10);")
				.containsOne("method(th, 100);")
				.containsOne("method((Throwable) e, 1000);")
				.containsOne("method((Exception) th, 10000);")
				.doesNotContain("(Exception) e");
	}
}
