package jadx.tests.integration.switches;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

// issue #2359
public class TestSwitchOverStrings5 extends SmaliTest {
	@Test
	public void testSmali() {
		disableCompilation();
		allowWarnInCode();
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("case \"mp3_400_e5\"")
				.countString(9, "case ")
				.countString(1, "default:");
	}
}
