package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.RaungTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestDuplicatedBlockCondition extends RaungTest {

	@Test
	public void test() {
		allowWarnInCode(); // block is duplicated, see TestConditions22
		assertThat(getClassNodeFromRaung())
				.code()
				.containsOne("if (z3 || z4) {")
				.doesNotContain("!z3 || z4");
	}
}
