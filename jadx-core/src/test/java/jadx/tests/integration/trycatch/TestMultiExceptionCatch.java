package jadx.tests.integration.trycatch;

import java.security.ProviderException;
import java.time.DateTimeException;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestMultiExceptionCatch extends IntegrationTest {

	public static class TestCls {
		public void test() {
			try {
				System.out.println("Test");
			} catch (ProviderException | DateTimeException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("try {")
				.containsOne("} catch (ProviderException | DateTimeException e) {")
				.containsOne("throw new RuntimeException(e);")
				.doesNotContain("RuntimeException e;");
	}
}
