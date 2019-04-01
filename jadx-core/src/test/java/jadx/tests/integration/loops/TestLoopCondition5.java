package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static jadx.tests.api.utils.JadxMatchers.countString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;

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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("for ("));
		assertThat(code, containsOne("return -1;"));
		assertThat(code, countString(2, "return "));
	}

	@Test
	public void test1() {
		ClassNode cls = getClassNodeFromSmaliWithPath("loops", "TestLoopCondition5");
		String code = cls.getCode().toString();

		assertThat(code, anyOf(containsOne("for ("), containsOne("while (true) {")));
		assertThat(code, containsOne("return -1;"));
		assertThat(code, countString(2, "return "));
	}
}
