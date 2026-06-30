package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

/**
 * Test that a continue is not erroneously inserted where a break edge instruction already exists,
 * leading to the continue being lost and causing a decompile failure.
 */
public class TestBreakInLoop6 extends SmaliTest {
	@Test
	public void test() {
		disableCompilation();
		assertThat(getClassNodeFromSmali()).code().containsOne("break");
	}

}
