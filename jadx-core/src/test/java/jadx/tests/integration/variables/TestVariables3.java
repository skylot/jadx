package jadx.tests.integration.variables;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class TestVariables3 extends IntegrationTest {

	public static class TestCls {
		String test(Object s) {
			int i;
			if (s == null) {
				i = 2;
			} else {
				i = 3;
				s = null;
			}
			return s + " " + i;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("int i;"));
		assertThat(code, containsString("i = 2;"));
		assertThat(code, containsString("i = 3;"));
		assertThat(code, containsString("s = null;"));
		assertThat(code, containsString("return s + \" \" + i;"));
	}
}
