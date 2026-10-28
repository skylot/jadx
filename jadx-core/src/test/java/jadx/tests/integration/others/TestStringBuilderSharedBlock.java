package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.RaungTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestStringBuilderSharedBlock extends RaungTest {

	@Test
	public void test() {
		allowWarnInCode();
		assertThat(getClassNodeFromRaung())
				.code()
				.containsOne("Code duplicated, block:")
				.doesNotContain("Instruction removed from duplicated block")
				.doesNotContain("new StringBuilder")
				.countString(2, "+ \".bin\"");
	}

	@Test
	public void testSingleBlock() {
		assertThat(getClassNodeFromRaung("others/TestStringBuilderSingleBlock", "others.TestStringBuilderSingleBlock"))
				.code()
				.doesNotContain("JADX WARN")
				.doesNotContain("new StringBuilder")
				.containsOne("+ \".bin\"");
	}

	@Test
	public void testCrossBlockWarning() {
		allowWarnInCode();
		assertThat(getClassNodeFromRaung("others/TestStringBuilderCrossBlock", "others.TestStringBuilderCrossBlock"))
				.code()
				.containsOne("Instruction removed from duplicated block")
				.contains("Code duplicated, block:");
	}
}
