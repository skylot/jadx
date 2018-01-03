package jadx.tests.integration.trycatch;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestNestedTryCatch extends IntegrationTest {

	public static class TestCls {
		private void f() {
			try {
				Thread.sleep(1);
				try {
					Thread.sleep(2);
				} catch (InterruptedException e) {
				}
			} catch (Exception e) {
			}

			return;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("try {"));
		assertThat(code, containsString("Thread.sleep(1);"));
		assertThat(code, containsString("Thread.sleep(2);"));
		assertThat(code, containsString("} catch (InterruptedException e) {"));
		assertThat(code, containsString("} catch (Exception e2) {"));
		assertThat(code, not(containsString("return")));
	}
}
