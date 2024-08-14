package jadx.tests.integration.loops;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestBreakInComplexIf extends IntegrationTest {

	public static class TestCls {

		private int test(Map<String, Point> map, int mapX) {
			int length = 1;
			for (int x = mapX + 1; x < 100; x++) {
				Point tile = map.get(x + "");
				if (tile == null || tile.y != 100) {
					break;
				}
				length++;
			}
			return length;
		}

		class Point {
			public final int x;
			public final int y;

			Point(int x, int y) {
				this.x = x;
				this.y = y;
			}
		}

		public void check() {
			Map<String, Point> first = new HashMap<>();
			first.put("3", new Point(100, 100));
			first.put("4", new Point(60, 100));
			assertThat(test(first, 2)).isEqualTo(3);

			Map<String, Point> second = new HashMap<>();
			second.put("3", new Point(100, 100));
			second.put("4", new Point(60, 0)); // check break
			second.put("5", new Point(60, 100));
			assertThat(test(second, 2)).isEqualTo(2);
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("if (tile == null || tile.y != 100) {")
				.containsOne("break;");
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("break;");
	}
}
