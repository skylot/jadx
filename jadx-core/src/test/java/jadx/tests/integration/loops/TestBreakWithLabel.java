package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestBreakWithLabel extends IntegrationTest {

	public static class TestCls {

		public boolean test(int[][] arr, int b) {
			boolean found = false;
			loop0: for (int i = 0; i < arr.length; i++) {
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

		public void check() {
			int[][] testArray = { { 1, 2 }, { 3, 4 } };
			assertTrue(test(testArray, 3));
			assertFalse(test(testArray, 5));
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("loop0:"));
		assertThat(code, containsOne("break loop0;"));
	}
}
