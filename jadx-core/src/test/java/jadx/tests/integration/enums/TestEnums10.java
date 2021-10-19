package jadx.tests.integration.enums;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

/**
 * Some enum field was removed, but still exist in values array
 */
public class TestEnums10 extends SmaliTest {

	@Test
	public void test() {
		assertThat(getClassNodeFromSmali())
				.code()
				.doesNotContain("Failed to restore enum class")
				.containsOne("enum TestEnums10 {")
				.countString(4, "Fake field");
	}
}
