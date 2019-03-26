package jadx.tests.integration.trycatch;

import java.io.File;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("File output = null;"));
		assertThat(code, containsOne("output = File.createTempFile(\"f\", \"a\", "));
		assertThat(code, containsOne("return 0;"));
		assertThat(code, containsOne("} catch (Exception e) {"));
		assertThat(code, containsOne("if (output != null) {"));
		assertThat(code, containsOne("output.delete();"));
		assertThat(code, containsOne("return 2;"));
	}
}
