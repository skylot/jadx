package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.core.dex.visitors.SimplifyVisitor;
import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

/**
 * Test the StringBuilder simplification part of {@link SimplifyVisitor}
 *
 * @author Jan Peter Stotz
 */
@SuppressWarnings("StringBufferReplaceableByString")
public class TestStringBuilderElimination2 extends IntegrationTest {

	public static class TestCls1 {
		public String test() {
			return new StringBuilder("[init]").append("a1").append('c').append(2).append(0L).append(1.0f).append(2.0d).append(true)
					.toString();
		}
	}

	@Test
	public void test1() {
		JadxAssertions.assertThat(getClassNode(TestCls1.class))
				.code()
				.contains("return \"[init]a1c201.02.0true\";");
	}

	public static class TestCls2 {
		public String test() {
			// A chain with non-final variables
			String sInit = "[init]";
			String s = "a1";
			char c = 'c';
			int i = 1;
			long l = 2;
			float f = 1.0f;
			double d = 2.0d;
			boolean b = true;
			return new StringBuilder(sInit).append(s).append(c).append(i).append(l).append(f).append(d).append(b).toString();
		}
	}

	@Test
	public void test2() {
		JadxAssertions.assertThat(getClassNode(TestCls2.class))
				.code()
				.contains("return \"[init]a1c121.02.0true\";");
	}

	public static class TestClsStringUtilsReverse {

		/**
		 * Simplified version of org.apache.commons.lang3.StringUtils.reverse()
		 */
		public static String reverse(final String str) {
			return new StringBuilder(str).reverse().toString();
		}
	}

	@Test
	public void test3() {
		JadxAssertions.assertThat(getClassNode(TestClsStringUtilsReverse.class))
				.code()
				.contains("return new StringBuilder(str).reverse().toString();");
	}

	public static class TestClsChainWithDelete {
		public String test() {
			// a chain we can't simplify
			return new StringBuilder("[init]").append("a1").delete(1, 2).toString();
		}
	}

	@Test
	public void testChainWithDelete() {
		JadxAssertions.assertThat(getClassNode(TestClsChainWithDelete.class))
				.code()
				.contains("return new StringBuilder(\"[init]\").append(\"a1\").delete(1, 2).toString();");
	}
}
