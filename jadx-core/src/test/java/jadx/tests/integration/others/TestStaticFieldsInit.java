package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("public static final String S2 = null;")));
		assertThat(code, containsString("public static final String S3 = null;"));
	}
}
