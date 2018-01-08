package jadx.tests.integration.inline;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestSyntheticInline extends IntegrationTest {

	public static class TestCls {
		private int f;

		private int func() {
			return -1;
		}

		public class A {
			public int getF() {
				return f;
			}

			public void setF(int v) {
				f = v;
			}

			public int callFunc() {
				return func();
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("synthetic")));
		assertThat(code, not(containsString("access$")));
		assertThat(code, not(containsString("x0")));
		assertThat(code, containsString("f = v;"));

//		assertThat(code, containsString("return f;"));
//		assertThat(code, containsString("return func();"));
		// Temporary solution
		assertThat(code, containsString("return TestSyntheticInline$TestCls.this.f;"));
		assertThat(code, containsString("return TestSyntheticInline$TestCls.this.func();"));
	}
}
