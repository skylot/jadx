package jadx.tests.integration.arrays;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class TestArrayFill2 extends IntegrationTest {

	public static class TestCls {

		public int[] test(int a) {
			return new int[]{1, a + 1, 2};
		}

		// TODO
//		public int[] test2(int a) {
//			return new int[]{1, a++, a * 2};
//		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("return new int[]{1, a + 1, 2};"));

		// TODO
		// assertThat(code, containsString("return new int[]{1, a++, a * 2};"));
	}
}
