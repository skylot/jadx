package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestConditions11 extends IntegrationTest {

	public static class TestCls {

		public void test(boolean a, int b) {
			if (a || b > 2) {
				f();
			}
		}

		private void f() {
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("if (a || b > 2) {"));
		assertThat(code, containsOne("f();"));
		assertThat(code, not(containsString("return")));
		assertThat(code, not(containsString("else")));
	}
}
