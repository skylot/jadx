package jadx.tests.integration.loops;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import org.junit.Test;

import static jadx.tests.api.utils.JadxMatchers.containsLines;
import static org.junit.Assert.assertThat;

public class TestIndexForLoop extends IntegrationTest {

	public static class TestCls {

		private int test(int[] a, int b) {
			int sum = 0;
			for (int i = 0; i < b; i++) {
				sum += a[i];
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
				"for (int i = 0; i < b; i++) {",
				indent(1) + "sum += a[i];",
				"}",
				"return sum;"
		));
	}
}
