package jadx.tests.integration.invoke;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

/**
 * Test cast for 'unknown' but overloaded method
 */
public class TestCastInOverloadedInvoke3 extends IntegrationTest {

	public static class OuterCls {
		static int c = 0;

		public static void call(String str) {
			c = 1;
		}

		public static void call(List<String> list) {
			c = 10;
		}
	}

	public static class TestCls {
		public void test() {
			OuterCls.call((String) null);
		}
	}

	@Test
	public void test() {
		disableCompilation();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("OuterCls.call((String) null);");
	}
}
