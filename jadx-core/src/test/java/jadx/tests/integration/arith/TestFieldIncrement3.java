package jadx.tests.integration.arith;

import java.util.Random;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class TestFieldIncrement3 extends IntegrationTest {

	public static class TestCls {
		static int tileX;
		static int tileY;
		static Vector2 targetPos = new Vector2();
		static Vector2 directVect = new Vector2();
		static Vector2 newPos = new Vector2();

		private static void test() {
			Random rd = new Random();
			int direction = rd.nextInt(7);
			switch (direction) {
				case 0:
					targetPos.x = (float) (((tileX + 1) * 55) + 55);
					targetPos.y = (float) (((tileY + 1) * 35) + 35);
					break;
				case 2:
					targetPos.x = (float) (((tileX + 1) * 55) + 55);
					targetPos.y = (float) (((tileY - 1) * 35) + 35);
					break;
				case 4:
					targetPos.x = (float) (((tileX - 1) * 55) + 55);
					targetPos.y = (float) (((tileY - 1) * 35) + 35);
					break;
				case 6:
					targetPos.x = (float) (((tileX - 1) * 55) + 55);
					targetPos.y = (float) (((tileY + 1) * 35) + 35);
					break;
				default:
					break;
			}
			directVect.x = targetPos.x - newPos.x;
			directVect.y = targetPos.y - newPos.y;

			float hPos = (float) Math.sqrt((double) ((directVect.x * directVect.x) + (directVect.y * directVect.y)));
			directVect.x /= hPos;
			directVect.y /= hPos;
		}

		static class Vector2 {
			public float x;
			public float y;

			public Vector2() {
				this.x = 0.0f;
				this.y = 0.0f;
			}

			public boolean equals(Vector2 other) {
				return (this.x == other.x && this.y == other.y);
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("directVect.x = targetPos.x - newPos.x;"));
	}
}
