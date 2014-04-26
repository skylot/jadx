package jadx.tests.internal.conditions;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestConditions7 extends InternalJadxTest {

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
		System.out.println(code);

		assertThat(code, containsString("if (i >= 0 && i < a.length) {"));
		assertThat(code, not(containsString("||")));
	}
}
