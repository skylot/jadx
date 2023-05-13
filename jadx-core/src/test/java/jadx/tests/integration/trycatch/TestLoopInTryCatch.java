package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestLoopInTryCatch extends SmaliTest {
	@Test
	public void test() {
		assertThat(getClassNodeFromSmali())
				.code()
				.oneOf(c -> c.containsLines(2,
						"int i;",
						"while (true) {",
						"    try {",
						"        i = getI();",
						"    } catch (RuntimeException unused) {",
						"        return;",
						"    }",
						"    if (i == 1 || i == 2) {",
						"        break;",
						"    }",
						"}",
						"if (i != 1) {",
						"    getI();",
						"}"),
						c -> c.containsLines(2,
								"int i;",
								"while (true) {",
								"    try {",
								"        i = getI();",
								"        if (i == 1 || i == 2) {",
								"            break;",
								"        }",
								"    } catch (RuntimeException unused) {",
								"        return;",
								"    }",
								"}",
								"if (i != 1) {",
								"    getI();",
								"}"),
						// TODO: weird result but correct, better to not use do-while if not really needed
						c -> c.containsLines(2,
								"int i;",
								"do {",
								"    try {",
								"        i = getI();",
								"        if (i == 1) {",
								"            break;",
								"        }",
								"    } catch (RuntimeException unused) {",
								"        return;",
								"    }",
								"} while (i != 2);",
								"if (i != 1) {",
								"    getI();",
								"}"));
	}
}
