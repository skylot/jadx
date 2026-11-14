package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTryCatch12 extends SmaliTest {

	// @formatter:off
	/*
		public static int test(TestTryCatch12 t) {
			try {
				TestTryCatch12 r = t.get("key");
				if (r != null) {
					return r.getInt("level", -1);
				}
				log("TAG", "Not found");
			} catch (Exception e) {
				logError("TAG", "Error", e);
			}
			return -1;
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		assertThat(getClassNodeFromSmali())
				.code()
				.containsLines(2,
						"try {",
						indent(1) + "TestTryCatch12 testTryCatch13 = testTryCatch12.get(\"key\");",
						indent(1) + "if (testTryCatch13 != null) {",
						indent(2) + "return testTryCatch13.getInt(\"level\", -1);",
						indent(1) + "}",
						indent(1) + "log(\"TAG\", \"Not found\");",
						"} catch (Exception e) {",
						indent(1) + "logError(\"TAG\", \"Error\", e);",
						"}",
						"return -1;");
	}
}
