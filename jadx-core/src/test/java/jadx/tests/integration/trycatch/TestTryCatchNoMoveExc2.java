package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.JadxMatchers.containsLines;
import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Issue: https://github.com/skylot/jadx/issues/395
 */
public class TestTryCatchNoMoveExc2 extends SmaliTest {
	// @formatter:off
	/*
		private static void test(AutoCloseable closeable) {
			if (closeable != null) {
				try {
					closeable.close();
				} catch (Exception unused) {
				}
				System.nanoTime();
			}
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		ClassNode cls = getClassNodeFromSmaliWithPkg("trycatch", "TestTryCatchNoMoveExc2");
		String code = cls.getCode().toString();

		assertThat(code, containsOne("try {"));
		assertThat(code, containsLines(2,
				"} catch (Exception unused) {",
				"}",
				"System.nanoTime();"));
	}
}
