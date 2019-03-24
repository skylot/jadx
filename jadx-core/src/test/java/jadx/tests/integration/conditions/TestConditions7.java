package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestConditions7 extends IntegrationTest {

	public static class TestCls {
		public void test(int[] a, int i) {
			if (i >= 0 && i < a.length) {
				a[i]++;
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("if (i >= 0 && i < a.length) {"));
		assertThat(code, not(containsString("||")));
	}
}
