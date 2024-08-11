package jadx.tests.integration.arith;

import java.util.Random;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestFieldIncrement3 extends IntegrationTest {

	public static class TestCls {
		static int tileX;
		static int tileY;
		static Vector2 targetPos = new Vector2();
		static Vector2 directVect = new Vector2();
		static Vector2 newPos = new Vector2();

		public static void test() {
			Random rd = new Random();
			int direction = rd.nextInt(7);
			switch (direction) {
				case 0:
					targetPos.x = ((tileX + 1) * 55) + 55;
					targetPos.y = ((tileY + 1) * 35) + 35;
					break;
				case 2:
					targetPos.x = ((tileX + 1) * 55) + 55;
					targetPos.y = ((tileY - 1) * 35) + 35;
					break;
				case 4:
					targetPos.x = ((tileX - 1) * 55) + 55;
					targetPos.y = ((tileY - 1) * 35) + 35;
					break;
				case 6:
					targetPos.x = ((tileX - 1) * 55) + 55;
					targetPos.y = ((tileY + 1) * 35) + 35;
					break;
				default:
					break;
			}
			directVect.x = targetPos.x - newPos.x;
			directVect.y = targetPos.y - newPos.y;

			float hPos = (float) Math.sqrt((directVect.x * directVect.x) + (directVect.y * directVect.y));
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
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("directVect.x = targetPos.x - newPos.x;");
	}
}
