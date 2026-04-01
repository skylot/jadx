package jadx.tests.integration.variables;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestVariables7 extends SmaliTest {
	@Test
	public void testNoDebug() {
		getArgs().setDebugInfo(false);
		assertThat(getClassNodeFromSmali())
				.code()
				.doesNotContain("r0");
	}
}
