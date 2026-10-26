package jadx.tests.integration.switches;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSwitchSharedCaseTargets extends SmaliTest {
	@Test
	public void testSmali() {
		allowWarnInCode();
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("case 3:")
				.containsOne("if (i2 != 3) {")
				.containsOne("} else {")
				.containsOne("stop();")
				.countString(2, "fail();")
				.countString(2, "complete();");
	}
}
