package jadx.tests.internal.inline;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestInline3 extends InternalJadxTest {

	public static class TestCls {
		public TestCls(int b1, int b2) {
			this(b1, b2, 0, 0, 0);
		}

		public TestCls(int a1, int a2, int a3, int a4, int a5) {
		}

		public class A extends TestCls{
			public A(int a) {
				super(a, a);
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("this(b1, b2, 0, 0, 0);"));
		assertThat(code, containsString("super(a, a);"));
		assertThat(code, not(containsString("super(a, a).this$0")));

		assertThat(code, containsString("public class A extends TestInline3$TestCls {"));
	}
}
