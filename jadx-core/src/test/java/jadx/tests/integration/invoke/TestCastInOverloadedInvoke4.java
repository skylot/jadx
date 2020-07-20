package jadx.tests.integration.invoke;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestCastInOverloadedInvoke4 extends IntegrationTest {

	public static class TestCls {
		public String test(String str) {
			return str.replace('\n', ' ');
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("return str.replace('\\n', ' ');");
	}
}
