package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

/**
 * Issue #2052
 */
public class TestIfCodeStyle2 extends SmaliTest {

	@Test
	public void testSmali() {
		disableCompilation();
		assertThat(getClassNodeFromSmali())
				.code()
				.countString(1, "} else if (")
				.countString(1, "} else {")
				.countString(19, "return ");
	}
}
