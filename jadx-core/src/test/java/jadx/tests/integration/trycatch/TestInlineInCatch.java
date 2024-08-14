package jadx.tests.integration.trycatch;

import java.io.File;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestInlineInCatch extends IntegrationTest {

	public static class TestCls {
		private File dir;

		public int test() {
			File output = null;
			try {
				output = File.createTempFile("f", "a", dir);
				if (!output.exists()) {
					return 1;
				}
				return 0;
			} catch (Exception e) {
				if (output != null) {
					output.delete();
				}
				return 2;
			}
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("File output = null;")
				.containsOne("output = File.createTempFile(\"f\", \"a\", ")
				.containsOne("return 0;")
				.containsOne("} catch (Exception e) {")
				.containsOne("if (output != null) {")
				.containsOne("output.delete();")
				.containsOne("return 2;");
	}
}
