package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestStringConcatWithoutResult extends IntegrationTest {
	private static final Logger LOG = LoggerFactory.getLogger(TestStringConcatWithoutResult.class);

	public static class TestCls {
		public static final boolean LOG_DEBUG = false;

		public void test(int i) {
			String msg = "Input arg value: " + i;
			if (LOG_DEBUG) {
				LOG.debug(msg);
			}
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne(" = \"Input arg value: \" + i;");
	}
}
