package jadx.tests.integration.loops;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsLines;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class TestIndexForLoop extends IntegrationTest {

	public static class TestCls {

		private int test(int[] a, int b) {
			int sum = 0;
			for (int i = 0; i < b; i++) {
				sum += a[i];
			}
			return sum;
		}

		public void check() {
			int[] array = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
			assertEquals(0, test(array, 0));
			assertEquals(6, test(array, 3));
			assertEquals(36, test(array, 8));
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsLines(2,
				"int sum = 0;",
				"for (int i = 0; i < b; i++) {",
				indent(1) + "sum += a[i];",
				"}",
				"return sum;"
		));
	}
}
