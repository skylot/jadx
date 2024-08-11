package jadx.tests.integration.types;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestTypeResolver2 extends IntegrationTest {

	public static class TestCls {

		public static boolean test(Object obj) throws IOException {
			if (obj != null) {
				return true;
			}
			throw new IOException();
		}
	}

	@Test
	public void test() {
		noDebugInfo();

		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("if (obj != null) {");
	}
}
