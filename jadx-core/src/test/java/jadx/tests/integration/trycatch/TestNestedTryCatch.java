package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestNestedTryCatch extends IntegrationTest {

	public static class TestCls {
		public void test() {
			try {
				Thread.sleep(1L);
				try {
					Thread.sleep(2L);
				} catch (InterruptedException ignored) {
					System.out.println(2);
				}
			} catch (Exception ignored) {
				System.out.println(1);
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("try {"));
		assertThat(code, containsString("Thread.sleep(1L);"));
		assertThat(code, containsString("Thread.sleep(2L);"));
		assertThat(code, containsString("} catch (InterruptedException e) {"));
		assertThat(code, containsString("} catch (Exception e2) {"));
		assertThat(code, not(containsString("return")));
	}
}
