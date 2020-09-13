package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

/**
 * Issue 921 (second case)
 */
public class TestTypeResolver15 extends SmaliTest {

	public static class TestCls {
		private void test(boolean z) {
			useInt(z ? 0 : 8);
			useInt(!z ? 1 : 0); // replaced with xor in smali test
		}

		private void useInt(int i) {
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				// .containsOne("useInt(!z ? 1 : 0);") // TODO: convert to ternary
				.containsOne("useInt(z ? 0 : 8);");
	}

	@Test
	public void testSmali() {
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("useInt(z ? 0 : 8);")
				.containsOne("useInt(!z ? 1 : 0);");
	}
}
