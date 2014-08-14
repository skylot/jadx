package jadx.tests.internal.arrays;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class TestArrayFill2 extends InternalJadxTest {

	public static class TestCls {

		public int[] test(int a) {
			return new int[]{1, a + 1, 2};
		}

		public int[] test2(int a) {
			return new int[]{1, a++, a * 2};
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
		System.out.println(code);

		assertThat(code, containsString("return new int[]{1, a + 1, 2};"));

		// TODO
		// assertThat(code, containsString("return new int[]{1, a++, a * 2};"));
	}
}
