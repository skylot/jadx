package jadx.tests.integration.usethis;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestInlineThis extends IntegrationTest {

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

		assertThat(code, not(containsString("something")));
		assertThat(code, not(containsString("something.method()")));
		assertThat(code, not(containsString("something.field")));
		assertThat(code, not(containsString("= this")));

		assertThat(code, containsOne("this.field = 123;"));
		assertThat(code, containsOne("method();"));
	}
}
