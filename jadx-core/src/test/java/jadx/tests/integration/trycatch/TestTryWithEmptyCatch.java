package jadx.tests.integration.trycatch;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("try {"));
		assertThat(code, containsOne("if ("));
	}
}
