package jadx.tests.integration.trycatch;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.JadxMatchers.containsLines;
import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertThat;

/**
 * Issue: https://github.com/skylot/jadx/issues/395
 */
public class TestTryCatchNoMoveExc2 extends SmaliTest {

//	private static void test(AutoCloseable closeable) {
//		if (closeable != null) {
//			try {
//				closeable.close();
//			} catch (Exception unused) {
//			}
//			System.nanoTime();
//		}
//	}

	@Test
	public void test() {
		ClassNode cls = getClassNodeFromSmaliWithPath("trycatch", "TestTryCatchNoMoveExc2");
		String code = cls.getCode().toString();

		assertThat(code, containsOne("try {"));
		assertThat(code, containsLines(2,
				"} catch (Exception unused) {",
				"}",
				"System.nanoTime();"
		));
	}
}
