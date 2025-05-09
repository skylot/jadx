package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

class TestInvalidExceptions2 extends SmaliTest {

	@Test
	void test() {
		allowWarnInCode();
		disableCompilation();
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("throwPossibleExceptionType() throws UnknownTypeHierarchyException {");
	}
}
