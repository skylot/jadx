package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

public class TestStringBuilderElimination3 extends IntegrationTest {

	public static class TestCls {
		public static String test(String a) {
			StringBuilder sb = new StringBuilder();
			sb.append("result = ");
			sb.append(a);
			return sb.toString();
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("return \"result = \" + a;"));
		assertThat(code, not(containsString("new StringBuilder()")));
	}

	public static class TestClsNegative {
		private String f = "first";

		public String test() {
			StringBuilder sb = new StringBuilder();
			sb.append("before = ");
			sb.append(this.f);
			updateF();
			sb.append(", after = ");
			sb.append(this.f);
			return sb.toString();
		}

		private void updateF() {
			this.f = "second";
		}

		public void check() {
			assertThat(test(), is("before = first, after = second"));
		}
	}

	@Test
	public void testNegative() {
		ClassNode cls = getClassNode(TestClsNegative.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("return sb.toString();"));
		assertThat(code, containsString("new StringBuilder()"));
	}
}
