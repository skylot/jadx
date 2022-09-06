package jadx.tests.integration.trycatch;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsLines;
import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestTryCatchFinally6 extends IntegrationTest {

	public static class TestCls {
		public static void test() throws IOException {
			InputStream is = null;
			try {
				call();
				is = new FileInputStream("1.txt");
			} finally {
				if (is != null) {
					is.close();
				}
			}
		}

		private static void call() {
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsLines(2,
				"InputStream is = null;",
				"try {",
				indent(1) + "call();",
				indent(1) + "is = new FileInputStream(\"1.txt\");",
				"} finally {",
				indent(1) + "if (is != null) {",
				indent(2) + "is.close();",
				indent(1) + '}',
				"}"));
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		// impossible to proof that variables should be merged, so can't restore finally block here
		assertThat(code, containsOne("if (0 != 0) {"));
	}
}
