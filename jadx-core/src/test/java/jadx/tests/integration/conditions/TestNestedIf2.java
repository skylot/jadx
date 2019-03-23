package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("if (executedCount != repeatCount && isRun(delta, object)) {"));
		assertThat(code, containsOne("if (finished) {"));
		assertThat(code, not(containsString("else")));
	}
}
