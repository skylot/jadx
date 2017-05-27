package jadx.tests.integration.loops;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

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
			Map<String, Point> map = new HashMap<>();
			map.put("3", new Point(100, 100));
			map.put("4", new Point(60, 100));
			assertThat(test(map, 2), is(3));
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("if (tile == null || tile.y != 100) {"));
		assertThat(code, containsOne("break;"));
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
		assertThat(code, containsOne("break;"));
	}
}
