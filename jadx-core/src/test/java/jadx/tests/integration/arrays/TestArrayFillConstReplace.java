package jadx.tests.integration.arrays;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertThat;

public class TestArrayFillConstReplace extends IntegrationTest {

	public static class TestCls {
		public static final int CONST_INT = 0xffff;

		public int[] test() {
			return new int[]{127, 129, CONST_INT};
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("return new int[]{127, 129, CONST_INT};"));
	}
}
