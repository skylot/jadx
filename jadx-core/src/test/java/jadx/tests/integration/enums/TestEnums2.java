package jadx.tests.integration.enums;

import org.junit.jupiter.api.Test;

import jadx.api.CommentsLevel;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestEnums2 extends IntegrationTest {

	public static class TestCls {

		public enum Operation {
			PLUS {
				@Override
				public int apply(int x, int y) {
					return x + y;
				}
			},
			MINUS {
				@Override
				public int apply(int x, int y) {
					return x - y;
				}
			};

			public abstract int apply(int x, int y);
		}
	}

	@Test
	public void test() {
		getArgs().setCommentsLevel(CommentsLevel.WARN);
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsLines(1,
						"public enum Operation {",
						indent(1) + "PLUS {",
						indent(2) + "@Override",
						indent(2) + "public int apply(int x, int y) {",
						indent(3) + "return x + y;",
						indent(2) + '}',
						indent(1) + "},",
						indent(1) + "MINUS {",
						indent(2) + "@Override",
						indent(2) + "public int apply(int x, int y) {",
						indent(3) + "return x - y;",
						indent(2) + '}',
						indent(1) + "};",
						"",
						indent(1) + "public abstract int apply(int i, int i2);",
						"}");
	}
}
