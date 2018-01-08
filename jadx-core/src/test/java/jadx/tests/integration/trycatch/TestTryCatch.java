package jadx.tests.integration.trycatch;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestTryCatch extends IntegrationTest {

	public static class TestCls {
		private void f() {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// ignore
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("try {"));
		assertThat(code, containsString("Thread.sleep(50);"));
		assertThat(code, containsString("} catch (InterruptedException e) {"));
		assertThat(code, not(containsString("return")));
	}
}
