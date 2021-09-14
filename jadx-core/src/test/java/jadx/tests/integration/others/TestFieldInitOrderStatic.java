package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestFieldInitOrderStatic extends IntegrationTest {

	@SuppressWarnings("ConstantName")
	public static class TestCls {
		private static final StringBuilder sb = new StringBuilder();
		private static final String a = sb.append("a").toString();
		private static final String b = sb.append("b").toString();
		private static final String c = sb.append("c").toString();
		private static final String result = sb.toString();

		public void check() {
			assertThat(result).isEqualTo("abc");
			assertThat(a).isEqualTo("a");
			assertThat(b).isEqualTo("ab");
			assertThat(c).isEqualTo("abc");
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("static {")
				.doesNotContain("String result;")
				.containsOne("String result = sb.toString();");
	}
}
