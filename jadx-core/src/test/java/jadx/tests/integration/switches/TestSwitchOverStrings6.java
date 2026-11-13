package jadx.tests.integration.switches;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

// issue #2884
public class TestSwitchOverStrings6 extends SmaliTest {
	@Test
	public void testSmali() {
		disableCompilation();
		assertThat(getClassNodeFromSmali())
				.code()
				.doesNotContain("JADX WARN: Failed to restore switch over string")
				.containsOne("switch (str) {")
				.containsOne("case \"a\":")
				.containsOne("case \"b\":")
				.doesNotContain("default:");
	}
}
