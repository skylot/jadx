package jadx.tests.integration.loops;

import jadx.tests.api.IntegrationTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static jadx.tests.api.utils.JadxMatchers.containsLines;
import static org.junit.Assert.assertThat;

public class TestIterableForEach extends IntegrationTest {

	public static class TestCls {
		private String test(Iterable<String> a) {
			StringBuilder sb = new StringBuilder();
			for (String s : a) {
				sb.append(s);
			}
			return sb.toString();
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();
		System.out.println(code);

		assertThat(code, containsLines(2,
				"StringBuilder sb = new StringBuilder();",
				"for (String s : a) {",
				indent(1) + "sb.append(s);",
				"}",
				"return sb.toString();"
		));
	}
}
