package jadx.tests.integration.loops;

import jadx.tests.api.IntegrationTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static jadx.tests.api.utils.JadxMatchers.containsLines;
import static org.junit.Assert.assertThat;

public class TestArrayForEach extends IntegrationTest {

	public static class TestCls {

		private int test(int[] a) {
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
		System.out.println(code);

		assertThat(code, containsLines(2,
				"int sum = 0;",
				"for (int n : a) {",
				indent(1) + "sum += n;",
				"}",
				"return sum;"
		));
	}
}
