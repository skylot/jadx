package jadx.tests.integration.invoke;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

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
			assertEquals(23212, c);
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("public void test(Throwable th, Exception e) {"));
		assertThat(code, containsOne("method(e, 10);"));
		assertThat(code, containsOne("method(th, 100);"));
		assertThat(code, containsOne("method((Throwable) e, 1000);"));
		assertThat(code, containsOne("method((Exception) th, 10000);"));
		assertThat(code, not(containsString("(Exception) e")));
	}
}
