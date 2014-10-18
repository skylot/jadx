package jadx.tests.integration.invoke;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import org.junit.Test;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class TestOverloadedMethodInvoke extends IntegrationTest {

	public static class TestCls {
		int c;

		public void method(Throwable th) {
			c++;
			if (th != null) {
				c+=100;
			}
		}

		public void method(Exception e) {
			c += 1000;
			if (e != null) {
				c += 10000;
			}
		}

		public void test(Throwable th, Exception e) {
			method(e);
			method(th);
			method((Throwable) e);
			method((Exception) th);
		}

		public void check() {
			test(null, new Exception());
			assertEquals(12102, c);
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("public void test(Throwable th, Exception e) {"));
		assertThat(code, containsOne("method(e);"));
		assertThat(code, containsOne("method(th);"));
		assertThat(code, containsOne("method((Throwable) e);"));
		assertThat(code, containsOne("method((Exception) th);"));
		assertThat(code, not(containsString("(Exception) e")));
	}
}
