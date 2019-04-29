package jadx.tests.integration.arrays;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestMultiDimArrayFill extends IntegrationTest {

	public static class TestCls {

		public static Obj test(int a, int b) {
			return new Obj(
					new int[][] {
							new int[] { 1 },
							new int[] { 2 },
							{ 3 },
							new int[] { 4, 5 },
							new int[0]
					},
					new int[] { a, a, a, a, b });
		}

		private static class Obj {
			public Obj(int[][] ints, int[] ints2) {
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("return new Obj("
				+ "new int[][]{new int[]{1}, new int[]{2}, new int[]{3}, new int[]{4, 5}, new int[0]}, "
				+ "new int[]{a, a, a, a, b});"));
	}
}
