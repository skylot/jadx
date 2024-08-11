package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

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
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("return \"result = \" + a;")
				.doesNotContain("new StringBuilder()");
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
			assertThat(test()).isEqualTo("before = first, after = second");
		}
	}

	@Test
	public void testNegative() {
		JadxAssertions.assertThat(getClassNode(TestClsNegative.class))
				.code()
				.contains("return sb.toString();")
				.contains("new StringBuilder()");
	}
}
