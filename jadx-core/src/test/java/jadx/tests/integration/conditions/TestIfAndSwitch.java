package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestIfAndSwitch extends SmaliTest {

	/* @formatter:off
		private final static int C = 0;

		private static int i;
		private static final Random rd = new Random();
		private static final int ACTION_MOVE = 2;

		public static boolean ifAndSwitch() {
			boolean update = false;
			if (rd.nextInt() == ACTION_MOVE) {
				switch (i) {
					case C:
						update = true;
						break;
				}
			}
			if (update) {
				return true;
			}
			return false;
		}
	@formatter:on */

	@Test
	public void test() {
		allowWarnInCode();
		JadxAssertions.assertThat(getClassNodeFromSmali())
				.code()
				.countString(1, "if (rd.nextInt() == ACTION_MOVE) {")
				.countString(1, "switch (")
				.countString(1, "else {");

	}

}
