package jadx.tests.integration.types;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestTypeResolver extends IntegrationTest {

	public static class TestCls {
		public TestCls(int b1, int b2) {
			// test 'this' move and constructor invocation on moved register
			this(b1, b2, 0, 0, 0);
		}

		public TestCls(int a1, int a2, int a3, int a4, int a5) {
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("this(b1, b2, 0, 0, 0);"));
		assertThat(code, not(containsString("= this;")));
	}
}
