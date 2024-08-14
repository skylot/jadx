package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTryCatchNoMoveExc extends SmaliTest {
	// @formatter:off
	/*
		private static void test(AutoCloseable closeable) {
			if (closeable != null) {
				try {
					closeable.close();
				} catch (Exception ignored) {
				}
			}
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		assertThat(getClassNodeFromSmaliWithPkg("trycatch", "TestTryCatchNoMoveExc"))
				.code()
				.containsOne("if (autoCloseable != null) {")
				.containsOne("try {")
				.containsOne("autoCloseable.close();");
	}
}
