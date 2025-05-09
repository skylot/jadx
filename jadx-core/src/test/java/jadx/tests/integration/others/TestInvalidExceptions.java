package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

class TestInvalidExceptions extends SmaliTest {

	@Test
	void test() {
		allowWarnInCode();
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("invalidException() throws FileNotFoundException {")
				.containsOne("Byte code manipulation detected: skipped illegal throws declaration")
				.removeBlockComments()
				.doesNotContain("String");
	}
}
