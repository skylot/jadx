package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.RaungTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

/**
 * Issue #2909
 */
public class TestOutBlock2 extends RaungTest {

	@Test
	public void test() {
		forceDecompiledCheck();
		assertThat(getClassNodeFromRaung())
				.code()
				.doesNotContain("Code duplicated");
	}
}
