package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

/**
 * Issue #2384
 */
public class TestOutBlock extends SmaliTest {

	@Test
	public void test() {
		allowWarnInCode();
		disableCompilation();
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("setContentView");
	}
}
