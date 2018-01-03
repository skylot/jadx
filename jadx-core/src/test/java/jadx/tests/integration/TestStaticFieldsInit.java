package jadx.tests.integration;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestStaticFieldsInit extends IntegrationTest {

	public static class TestCls {
		public static final String s1 = "1";
		public static final String s2 = "12".substring(1);
		public static final String s3 = null;
		public static final String s4;
		public static final String s5 = "5";
		public static String s6 = "6";

		static {
			if (s5.equals("?")) {
				s4 = "?";
			} else {
				s4 = "4";
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("public static final String s2 = null;")));
		// TODO:
		// assertThat(code, containsString("public static final String s3 = null;"));
	}
}
