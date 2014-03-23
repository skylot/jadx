package jadx.tests.internal.inner;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestInnerClass3 extends InternalJadxTest {

	public static class TestCls {
		private String c;

		private void setC(String c) {
			this.c = c;
		}

		public class C {
			public String c() {
				setC("c");
				return c;
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
		System.out.println(code);

		assertThat(code, not(containsString("synthetic")));
		assertThat(code, not(containsString("access$")));
		assertThat(code, not(containsString("x0")));
		assertThat(code, containsString("setC(\"c\");"));
	}
}
