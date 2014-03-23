package jadx.tests.internal.conditions;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestConditions4 extends InternalJadxTest {

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
		System.out.println(code);

		assertThat(code, containsString("num >= 59 && num <= 66"));
		assertThat(code, containsString("return inRange ? num + 1 : num;"));
		assertThat(code, not(containsString("else")));
	}
}
