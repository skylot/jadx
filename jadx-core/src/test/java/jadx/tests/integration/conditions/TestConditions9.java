package jadx.tests.integration.conditions;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestConditions9 extends IntegrationTest {

	public static class TestCls {
		public void test(boolean a, int b) throws Exception {
			if (!a || (b >= 0 && b <= 11)) {
				System.out.println('1');
			} else {
				System.out.println('2');
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("if (!a || (b >= 0 && b <= 11)) {"));
		assertThat(code, containsOne("System.out.println('1');"));
		assertThat(code, containsOne("} else {"));
		assertThat(code, containsOne("System.out.println('2');"));
		assertThat(code, not(containsString("return;")));
	}
}
