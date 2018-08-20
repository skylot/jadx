package jadx.tests.integration.trycatch;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertThat;

public class TestTryCatchNoMove extends SmaliTest {

//	private static void test(AutoCloseable closeable) {
//		if (closeable != null) {
//			try {
//				closeable.close();
//			} catch (Exception ignored) {
//			}
//		}
//	}

	@Test
	public void test() {
		ClassNode cls = getClassNodeFromSmaliWithPath("trycatch", "TestTryCatchNoMove");
		String code = cls.getCode().toString();

		assertThat(code, containsOne("if (autoCloseable != null) {"));
		assertThat(code, containsOne("try {"));
		assertThat(code, containsOne("autoCloseable.close();"));
	}
}
