package jadx.tests.integration.inner;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestInnerClass extends IntegrationTest {

	public static class TestCls {
		public class Inner {
			public class Inner2 extends Thread {
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("Inner {"));
		assertThat(code, containsString("Inner2 extends Thread {"));
		assertThat(code, not(containsString("super();")));
		assertThat(code, not(containsString("this$")));
		assertThat(code, not(containsString("/* synthetic */")));
	}
}
