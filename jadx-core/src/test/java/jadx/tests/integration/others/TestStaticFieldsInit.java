package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestStaticFieldsInit extends IntegrationTest {

	public static class TestCls {
		public static final String S1 = "1";
		public static final String S2 = "12".substring(1);
		public static final String S3 = null;
		public static final String S4;
		public static final String S5 = "5";
		public static String s6 = "6";

		static {
			if (S5.equals("?")) {
				S4 = "?";
			} else {
				S4 = "4";
			}
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("public static final String S2 = null;")
				.contains("public static final String S3 = null;");
	}
}
