package jadx.tests.integration.arrays;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestArrayFill2 extends IntegrationTest {

	public static class TestCls {

		public int[] test(int a) {
			return new int[] { 1, a + 1, 2 };
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("return new int[]{1, a + 1, 2};"));
	}

	public static class TestCls2 {

		public int[] test2(int a) {
			return new int[] { 1, a++, a * 2 };
		}
	}

	@Test
	@NotYetImplemented
	public void test2() {
		ClassNode cls = getClassNode(TestCls2.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("return new int[]{1, a++, a * 2};"));
	}
}
