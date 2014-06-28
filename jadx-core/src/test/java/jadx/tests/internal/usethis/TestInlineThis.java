package jadx.tests.internal.usethis;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static jadx.tests.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestInlineThis extends InternalJadxTest {

	public static class TestCls {
		public int field;

		private void test() {
			TestCls something = this;
			something.method();
			something.field = 123;
		}

		private void method() {
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
		System.out.println(code);

		assertThat(code, not(containsString("something")));
		assertThat(code, not(containsString("something.method()")));
		assertThat(code, not(containsString("something.field")));
		assertThat(code, not(containsString("= this")));

		assertThat(code, containsOne("this.field = 123;"));
		assertThat(code, containsOne("method();"));
	}
}
