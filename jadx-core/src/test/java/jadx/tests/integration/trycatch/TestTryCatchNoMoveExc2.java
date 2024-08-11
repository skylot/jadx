package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

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
		assertThat(getClassNodeFromSmaliWithPkg("trycatch", "TestTryCatchNoMoveExc2"))
				.code()
				.containsOne("try {")
				.containsLines(2,
						"} catch (Exception unused) {",
						"}",
						"System.nanoTime();");
	}
}
