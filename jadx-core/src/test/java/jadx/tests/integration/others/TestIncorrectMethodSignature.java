package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

/**
 * Issue #858.
 * Incorrect method signature change argument type and shift register numbers
 */
public class TestIncorrectMethodSignature extends SmaliTest {

	@Test
	public void test() {
		allowWarnInCode();
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("public TestIncorrectMethodSignature(String str) {");
	}
}
