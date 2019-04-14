package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

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
		ClassNode cls = getClassNodeFromSmaliWithPkg("trycatch", "TestTryCatchNoMoveExc");
		String code = cls.getCode().toString();

		assertThat(code, containsOne("if (autoCloseable != null) {"));
		assertThat(code, containsOne("try {"));
		assertThat(code, containsOne("autoCloseable.close();"));
	}
}
