package jadx.tests.integration.switches;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

// issue #2770
public class TestSwitchOverStrings4 extends SmaliTest {
	@Test
	public void testSmali() {
		disableCompilation();
		allowWarnInCode();
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("case \"DESKTOP\"")
				.countString(4, "case");
	}
}
