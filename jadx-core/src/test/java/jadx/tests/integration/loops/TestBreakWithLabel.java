package jadx.tests.integration.loops;

import java.lang.reflect.Method;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TestBreakWithLabel extends IntegrationTest {

	public static class TestCls {

		public boolean test(int[][] arr, int b) {
			boolean found = false;
			loop0:
			for (int i = 0; i < arr.length; i++) {
				for (int j = 0; j < arr[i].length; j++) {
					if (arr[i][j] == b) {
						found = true;
						break loop0;
					}
				}
			}
			System.out.println("found: " + found);
			return found;
		}
	}

	@Test
	public void test() throws Exception {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("loop0:"));
		assertThat(code, containsOne("break loop0;"));

		Method test = getReflectMethod("test", int[][].class, int.class);
		int[][] testArray = {{1, 2}, {3, 4}};
		assertTrue((Boolean) invoke(test, testArray, 3));
		assertFalse((Boolean) invoke(test, testArray, 5));
	}
}
