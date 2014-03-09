package jadx.tests.internal.arith;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class TestArith extends InternalJadxTest {

	public static class TestCls {

		public void method(int a) {
			a += 2;
		}

		public void method2(int a) {
			a++;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
		System.out.println(code);

		assertThat(code, containsString("a += 2;"));
		assertThat(code, containsString("a++;"));
	}
}
