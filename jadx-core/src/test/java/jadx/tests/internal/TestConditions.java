package jadx.tests.internal;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestConditions extends InternalJadxTest {

	public static class TestCls {
		private boolean f1(boolean a, boolean b, boolean c) {
			return (a && b) || c;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
		System.out.println(code);

		assertThat(code, not(containsString("(!a || !b) && !c")));
		assertThat(code, containsString("(a && b) || c"));
//		assertThat(code, containsString("return (a && b) || c;"));
	}
}
