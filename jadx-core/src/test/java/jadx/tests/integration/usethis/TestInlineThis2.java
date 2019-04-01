package jadx.tests.integration.usethis;

import java.util.Objects;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestInlineThis2 extends IntegrationTest {

	public static class TestCls {
		public int field;

		public void test() {
			TestCls thisVar = this;
			if (Objects.isNull(thisVar)) {
				System.out.println("null");
			}
			thisVar.method();
			thisVar.field = 123;
		}

		private void method() {
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("thisVar")));
		assertThat(code, not(containsString("thisVar.method()")));
		assertThat(code, not(containsString("thisVar.field")));
		assertThat(code, not(containsString("= this")));

		assertThat(code, containsOne("if (Objects.isNull(this)) {"));
		assertThat(code, containsOne("this.field = 123;"));
		assertThat(code, containsOne("method();"));
	}
}
