package jadx.tests.internal.inner;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestInnerClass extends InternalJadxTest {

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
		System.out.println(code);

		assertThat(code, containsString("Inner {"));
		assertThat(code, containsString("Inner2 extends Thread {"));
		assertThat(code, not(containsString("super();")));
		assertThat(code, not(containsString("this$")));
		assertThat(code, not(containsString("/* synthetic */")));
	}
}
