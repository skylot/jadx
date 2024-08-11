package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestLoopCondition5 extends SmaliTest {

	public static class TestCls {
		public static int lastIndexOf(int[] array, int target, int start, int end) {
			for (int i = end - 1; i >= start; i--) {
				if (array[i] == target) {
					return i;
				}
			}
			return -1;
		}
	}

	@Test
	public void test0() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("for (")
				.containsOne("return -1;")
				.countString(2, "return ");
	}

	@Test
	public void test1() {
		assertThat(getClassNodeFromSmaliWithPath("loops", "TestLoopCondition5"))
				.code()
				.containsOneOf("for (", "while (true) {", "} while (iArr[i3] != i);")
				.containsOne("return -1;")
				.countString(2, "return ");
	}
}
