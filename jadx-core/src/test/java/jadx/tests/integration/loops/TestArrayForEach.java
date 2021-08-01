package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsLines;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestArrayForEach extends IntegrationTest {

	public static class TestCls {

		public int test(int[] a) {
			int sum = 0;
			for (int n : a) {
				sum += n;
			}
			return sum;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsLines(2,
				"int sum = 0;",
				"for (int n : a) {",
				indent() + "sum += n;",
				"}",
				"return sum;"));
	}
}
