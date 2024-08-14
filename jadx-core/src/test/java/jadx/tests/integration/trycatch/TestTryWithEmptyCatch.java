package jadx.tests.integration.trycatch;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestTryWithEmptyCatch extends IntegrationTest {

	public static class TestCls extends Exception {
		private static final long serialVersionUID = -5723049816464070603L;
		private Properties field;

		public TestCls(String str) {
			super(str);
			Properties properties = null;
			try {
				if (str.contains("properties")) {
					properties = new Properties();
				}
			} catch (Exception unused) {
				// empty
			}
			this.field = properties;
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("try {")
				.containsOne("if (");
	}
}
