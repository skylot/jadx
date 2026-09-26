package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSharedIfBranchTarget extends SmaliTest {
	@Test
	public void testSmali() {
		allowWarnInCode();
		assertThat(getClassNodeFromSmali())
				.runDecompiledAutoCheck(this);
	}
}
