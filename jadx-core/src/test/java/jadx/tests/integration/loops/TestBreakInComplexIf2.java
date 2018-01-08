package jadx.tests.integration.loops;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.countString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class TestBreakInComplexIf2 extends IntegrationTest {

	public static class TestCls {

		private int test(List<String> list) {
			int length = 0;
			for (String str : list) {
				if (str.isEmpty() || str.length() > 4) {
					break;
				}
				if (str.equals("skip")) {
					continue;
				}
				if (str.equals("a")) {
					break;
				}
				length++;
			}
			return length;
		}

		public void check() {
			assertThat(test(Arrays.asList("x", "y", "skip", "z", "a")), is(3));
			assertThat(test(Arrays.asList("x", "skip", "")), is(1));
			assertThat(test(Arrays.asList("skip", "y", "12345")), is(1));
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, countString(2, "break;"));
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, countString(2, "break;"));
	}
}
