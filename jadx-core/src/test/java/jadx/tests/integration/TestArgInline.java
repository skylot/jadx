package jadx.tests.integration;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestArgInline extends IntegrationTest {

	public static class TestCls {

		public void test(int a) {
			while (a < 10) {
				int b = a + 1;
				a = b;
			}
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("i++;"));
		assertThat(code, not(containsString("i = i + 1;")));
	}
}
