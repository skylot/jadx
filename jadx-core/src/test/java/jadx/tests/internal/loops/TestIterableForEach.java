package jadx.tests.internal.loops;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static jadx.tests.utils.JadxMatchers.containsLines;
import static org.junit.Assert.assertThat;

public class TestIterableForEach extends InternalJadxTest {

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
