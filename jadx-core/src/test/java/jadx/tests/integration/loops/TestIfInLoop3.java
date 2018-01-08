package jadx.tests.integration.loops;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TestIfInLoop3 extends IntegrationTest {

	public static class TestCls {
		static boolean[][] occupied = new boolean[70][70];
		static boolean placingStone = true;

		private static boolean test(int xx, int yy) {
			int[] extraArray = new int[]{10, 45, 50, 50, 20, 20};
			if (extraArray != null && placingStone) {
				for (int i = 0; i < extraArray.length; i += 2) {
					int tX;
					int tY;
					if (yy % 2 == 0) {
						if (extraArray[i + 1] % 2 == 0) {
							tX = xx + extraArray[i];
						} else {
							tX = extraArray[i] + xx - 1;
						}
						tY = yy + extraArray[i + 1];
					} else {
						tX = xx + extraArray[i];
						tY = yy + extraArray[i + 1];
					}
					if (tX < 0 || tY < 0 || tY % 2 != 0 && tX > 28 || tY > 70
							|| occupied[tX][tY]) {
						return false;
					}
				}
			}
			return true;
		}

		public void check() {
			assertTrue(test(14, 2));
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("for (int i = 0; i < extraArray.length; i += 2) {"));
		assertThat(code, containsOne("if (extraArray != null && placingStone) {"));
	}
}
