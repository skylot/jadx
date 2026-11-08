package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

/**
 * Loop body exit jumps to a tail shared with code before the loop (issue #2781)
 */
public class TestLoopExitToSharedBlock extends SmaliTest {

	@Test
	public void test() {
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("log(\"TAG\", \"pass\");")
				.containsOne("log(\"TAG\", \"ignore\");")
				.countString(2, "return true;")
				.containsOne("return false;")
				.doesNotContain("str = \"pass\"");
	}
}
