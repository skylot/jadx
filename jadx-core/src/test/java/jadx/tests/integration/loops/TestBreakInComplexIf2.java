package jadx.tests.integration.loops;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

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
			assertThat(test(Arrays.asList("x", "y", "skip", "z", "a"))).isEqualTo(3);
			assertThat(test(Arrays.asList("x", "skip", ""))).isEqualTo(1);
			assertThat(test(Arrays.asList("skip", "y", "12345"))).isEqualTo(1);
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.countString(2, "break;");
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.countString(2, "break;");
	}
}
