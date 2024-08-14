package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestNestedIf2 extends IntegrationTest {

	public static class TestCls {
		static int executedCount = 0;
		static boolean finished = false;
		static int repeatCount = 2;

		static boolean test(float delta, Object object) {
			if (executedCount != repeatCount && isRun(delta, object)) {
				if (finished) {
					return true;
				}
				if (repeatCount == -1) {
					++executedCount;
					action();
					return false;
				}
				++executedCount;
				if (executedCount >= repeatCount) {
					return true;
				}
				action();
			}
			return false;
		}

		public static void action() {
		}

		public static boolean isRun(float delta, Object object) {
			return delta == 0;
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("if (executedCount != repeatCount && isRun(delta, object)) {")
				.containsOne("if (finished) {")
				.doesNotContain("else");
	}
}
