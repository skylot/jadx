package jadx.tests.integration.enums;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.JadxMatchers;

import static org.junit.Assert.assertThat;

public class TestEnums2 extends IntegrationTest {

	public static class TestCls {

		public enum Operation {
			PLUS {
				int apply(int x, int y) {
					return x + y;
				}
			},
			MINUS {
				int apply(int x, int y) {
					return x - y;
				}
			};

			abstract int apply(int x, int y);
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, JadxMatchers.containsLines(1,
				"public enum Operation {",
				indent(1) + "PLUS {",
				indent(2) + "int apply(int x, int y) {",
				indent(3) + "return x + y;",
				indent(2) + "}",
				indent(1) + "},",
				indent(1) + "MINUS {",
				indent(2) + "int apply(int x, int y) {",
				indent(3) + "return x - y;",
				indent(2) + "}",
				indent(1) + "};",
				"",
				indent(1) + "abstract int apply(int i, int i2);",
				"}"
		));
	}
}
