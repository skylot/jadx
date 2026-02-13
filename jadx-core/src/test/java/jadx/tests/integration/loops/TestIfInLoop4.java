package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestIfInLoop4 extends SmaliTest {

	/*
	 * Test handling of edge instructions when generated from a loop near an if statement with no else.
	 * They should not be added to an else region, since the if statement has no else.
	 * The actual condition here is less important than if decompilation succeeds at all.
	 */
	@Test
	public void test() {
		disableCompilation();
		assertThat(getClassNodeFromSmaliWithPath("loops", "TestIfInLoop4"))
				.code()
				.containsOne("return true;");
	}

}
