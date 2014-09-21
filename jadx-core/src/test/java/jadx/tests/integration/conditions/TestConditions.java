package jadx.tests.integration.conditions;

import jadx.tests.api.IntegrationTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestConditions extends IntegrationTest {

	public static class TestCls {
		private boolean test(boolean a, boolean b, boolean c) {
			return (a && b) || c;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
		System.out.println(code);

		assertThat(code, not(containsString("(!a || !b) && !c")));
		assertThat(code, containsString("return (a && b) || c;"));
	}
}
