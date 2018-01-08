package jadx.tests.integration.loops;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestLoopDetection extends IntegrationTest {

	public static class TestCls {

		private void test(int[] a, int b) {
			int i = 0;
			while (i < a.length && i < b) {
				a[i]++;
				i++;
			}
			while (i < a.length) {
				a[i]--;
				i++;
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("while (i < a.length && i < b) {"));
		assertThat(code, containsString("while (i < a.length) {"));

		assertThat(code, containsString("int i = 0;"));
		assertThat(code, not(containsString("i_2")));
		assertThat(code, containsString("i++;"));
	}
}
