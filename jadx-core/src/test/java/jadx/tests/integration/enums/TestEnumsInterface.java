package jadx.tests.integration.enums;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.JadxMatchers;

import static org.junit.Assert.assertThat;

public class TestEnumsInterface extends IntegrationTest {

	public static class TestCls {

		public enum Operation implements IOperation {
			PLUS {
				public int apply(int x, int y) {
					return x + y;
				}
			},
			MINUS {
				public int apply(int x, int y) {
					return x - y;
				}
			};
		}

		public interface IOperation {
			int apply(int x, int y);
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, JadxMatchers.containsLines(1,
				"public enum Operation implements IOperation {",
				indent(1) + "PLUS {",
				indent(2) + "public int apply(int x, int y) {",
				indent(3) + "return x + y;",
				indent(2) + "}",
				indent(1) + "},",
				indent(1) + "MINUS {",
				indent(2) + "public int apply(int x, int y) {",
				indent(3) + "return x - y;",
				indent(2) + "}",
				indent(1) + "}",
				"}"
		));
	}
}
