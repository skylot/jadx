package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.RaungTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTernarySameResult extends RaungTest {

	// Uses hand-written bytecode: javac folds 'if (c) return x; else return x;', so a plain Java
	// test can't produce the degenerate 'c ? x : x' ternary that this fix collapses.

	@Test
	public void test() {
		assertThat(getClassNodeFromRaung())
				.code()
				// same constant in both branches + side-effect-free condition => collapse
				.doesNotContain("? null : null")
				.doesNotContain("? 7 : 7")
				.doesNotContain("? \"x\" : \"x\"")
				.containsOne("return null;")
				.containsOne("return 7;")
				.containsOne("return \"x\";")
				// must keep the ternary: different values, a side effect in the condition,
				// or an operand whose cast would be dropped with the condition
				.containsOne("z ? 7 : 8")
				.containsOne("? 5 : 5")
				.containsOne("? 9 : 9");
	}
}
