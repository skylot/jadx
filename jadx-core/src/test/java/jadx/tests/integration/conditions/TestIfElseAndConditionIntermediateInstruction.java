package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

// Here there are two IF blocks for each part of the IF predicate.
// In some cases with optimised dex, two IF blocks cannot merged into the same IF region.
// This happens where there are intermediate instructions between the two blocks which cannot be
// inlined.
// Both IF blocks share the same ELSE block which is then added to both resultant regions.
// The resultant code does not reproduce the single if-else statement but it is better than failing
// to decompile.
public class TestIfElseAndConditionIntermediateInstruction extends SmaliTest {

	/* @formatter:off
		private boolean bool;
		private float num;
		private static final float CONST = 342;

		public void function() {
			if (bool && num < 1) {
				num += CONST;
			} else {
				nothing2();
			}
			nothing1();
		}

		private void nothing1() {

		}

		private void nothing2() {

		}
	@formatter:on */

	@Test
	public void test() {
		allowWarnInCode();
		JadxAssertions.assertThat(getClassNodeFromSmali())
				.code()
				.countString(2, "else")
				.countString(2, "nothing2();")
				.countString(1, "nothing1();")
				.countString(1, "if (this.bool)")
				.countString(1, "if (f < 1.0f)");
	}
}
