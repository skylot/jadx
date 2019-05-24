package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestConditions4 extends IntegrationTest {

	public static class TestCls {
		public int test(int num) {
			boolean inRange = (num >= 59 && num <= 66);
			return inRange ? num + 1 : num;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("num >= 59 && num <= 66"));
		assertThat(code, containsString("? num + 1 : num;"));
		assertThat(code, not(containsString("else")));
	}
}
