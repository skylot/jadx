package jadx.tests.integration;

import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.visitors.SimplifyVisitor;
import jadx.core.utils.exceptions.JadxException;
import jadx.tests.api.IntegrationTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

/**
 * Test the StringBuilder simplification part of {@link SimplifyVisitor}
 *
 * @author Jan Peter Stotz
 */
public class SimplifyVisitorStringBuilderTest extends IntegrationTest {

	public static class TestCls1 {
		public String test() {
			return new StringBuilder("[init]").append("a1").append('c').append(2).append(0l).append(1.0f).
					append(2.0d).append(true).toString();
		}
	}

	@Test
	public void test1() throws JadxException {
		ClassNode cls = getClassNode(SimplifyVisitorStringBuilderTest.TestCls1.class);
		SimplifyVisitor visitor = new SimplifyVisitor();
		visitor.visit(cls);
		String code = cls.getCode().toString();
		assertThat(code, containsString("return \"[init]\" + \"a1\" + 'c' + 2 + 0 + 1.0f + 2.0d + true;"));
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
			return new StringBuilder(sInit).append(s).append(c).append(i).append(l).append(f).
					append(d).append(b).toString();
		}
	}

	@Test
	public void test2() throws JadxException {
		ClassNode cls = getClassNode(SimplifyVisitorStringBuilderTest.TestCls2.class);
		SimplifyVisitor visitor = new SimplifyVisitor();
		visitor.visit(cls);
		String code = cls.getCode().toString();
		assertThat(code, containsString("return \"[init]\" + \"a1\" + 'c' + 1 + 2 + 1.0f + 2.0d + true;"));
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
	public void test3() throws JadxException {
		ClassNode cls = getClassNode(SimplifyVisitorStringBuilderTest.TestClsStringUtilsReverse.class);
		SimplifyVisitor visitor = new SimplifyVisitor();
		visitor.visit(cls);
		String code = cls.getCode().toString();
		assertThat(code, containsString("return new StringBuilder(str).reverse().toString();"));
	}

	public static class TestClsChainWithDelete {
		public String test() {
			// a chain we can't simplify
			return new StringBuilder("[init]").append("a1").delete(1, 2).toString();
		}
	}

	@Test
	public void testChainWithDelete() throws JadxException {
		ClassNode cls = getClassNode(TestClsChainWithDelete.class);
		SimplifyVisitor visitor = new SimplifyVisitor();
		visitor.visit(cls);
		String code = cls.getCode().toString();
		assertThat(code, containsString("return new StringBuilder(\"[init]\").append(\"a1\").delete(1, 2).toString();"));
	}
}
